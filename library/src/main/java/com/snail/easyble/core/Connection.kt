package com.snail.easyble.core

import android.bluetooth.*
import android.os.Build
import android.os.Message
import android.support.annotation.UiThread
import android.util.Log
import com.snail.easyble.callback.ConnectionStateChangeListener
import com.snail.easyble.event.Events
import com.snail.easyble.util.BleUtils

/**
 * Used for handling connection state and auto reconnect etc.
 * 
 * date: 2018/4/11 15:29
 * author: zengfansheng
 */
class Connection private constructor(device: Device, bluetoothDevice: BluetoothDevice, config: ConnectionConfig) : BaseConnection(device, bluetoothDevice, config) {

    private var stateChangeListener: ConnectionStateChangeListener? = null
    private var connStartTime = 0L //Used for connection timeout
    private var refreshTimes = 0 //Refresh times. clear if the server is successfully discovered
    private var tryReconnectTimes = 0 //Try to reconnect times
    private var lastConnectState = -1 
    private var reconnectImmediatelyCount = 0 //reconnection count without scanning
    private var refreshing = false
    private var isActiveDisconnect = false

    internal val isAutoReconnectEnabled: Boolean
        get() = config.isAutoReconnect

    val connctionState: Int
        get() = device.connectionState

    @Synchronized
    internal fun onScanResult(addr: String) {
        if (!isReleased && device.addr == addr && device.connectionState == IConnection.STATE_SCANNING) {
            connHandler.sendEmptyMessage(BaseConnection.MSG_CONNECT)
        }
    }

    @UiThread
    override fun handleMsg(msg: Message) {
        if (isReleased && msg.what != BaseConnection.MSG_RELEASE) {
            return
        }
        when (msg.what) {
            BaseConnection.MSG_CONNECT //do connect
            -> if (bluetoothAdapter!!.isEnabled) {
                doConnect()
            }
            BaseConnection.MSG_DISCONNECT //handle disconnect
            -> doDisconnect(msg.arg1 == MSG_ARG_RECONNECT && bluetoothAdapter!!.isEnabled, true)
            BaseConnection.MSG_REFRESH //manual refresh
            -> doRefresh(false)
            BaseConnection.MSG_AUTO_REFRESH //auto refresh
            -> doRefresh(true)
            BaseConnection.MSG_RELEASE //destroy the connection
            -> {
                config.setAutoReconnect(false) //stop auto reconnect
                doDisconnect(false, msg.arg1 == MSG_ARG_NOTIFY)
            }
            BaseConnection.MSG_TIMER
            -> doTimer()
            BaseConnection.MSG_DISCOVER_SERVICES, //do discover remote services
            BaseConnection.MSG_ON_CONNECTION_STATE_CHANGE, //GATT client has connected/disconnected to/from a remote GATT server.
            BaseConnection.MSG_ON_SERVICES_DISCOVERED //remote services have been discovered
            -> if (bluetoothAdapter!!.isEnabled) {
                if (msg.what == BaseConnection.MSG_DISCOVER_SERVICES) {
                    doDiscoverServices()
                } else {
                    if (msg.what == BaseConnection.MSG_ON_SERVICES_DISCOVERED) {
                        doOnServicesDiscovered(msg.arg1)
                    } else {
                        doOnConnectionStateChange(msg.arg1, msg.arg2)
                    }
                }
            }
        }
    }

    private fun notifyDisconnected() {
        device.connectionState = IConnection.STATE_DISCONNECTED
        sendConnectionCallback()
    }

    private fun doOnConnectionStateChange(status: Int, newState: Int) {
        if (bluetoothGatt != null) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Ble.println(javaClass, Log.DEBUG, "connected! [name: ${device.name}, addr: ${device.addr}]")
                    device.connectionState = IConnection.STATE_CONNECTED
                    sendConnectionCallback()
                    // Discovers services after a delay
                    connHandler.sendEmptyMessageDelayed(BaseConnection.MSG_DISCOVER_SERVICES, config.discoverServicesDelayMillis.toLong())
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Ble.println(javaClass, Log.DEBUG, "disconnected! [name: ${device.name}, addr: ${device.addr}, autoReconnEnable: ${config.isAutoReconnect}]")
                    clearRequestQueueAndNotify()
                    notifyDisconnected()
                }
            } else {
                Ble.println(javaClass, Log.ERROR, "GATT error! [name: ${device.name}, addr: ${device.addr}, status: $status]")
                if (status == 133) {
                    doClearTaskAndRefresh()
                } else {
                    clearRequestQueueAndNotify()
                    notifyDisconnected()
                }
            }
        }
    }

    private fun doOnServicesDiscovered(status: Int) {
        if (bluetoothGatt != null) {
            val services = bluetoothGatt!!.services
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Ble.println(javaClass, Log.DEBUG, "services discovered! [name: ${device.name}, addr: ${device.addr}, size: ${bluetoothGatt!!.services.size}]")
                if (services.isEmpty()) {
                    doClearTaskAndRefresh()
                } else {
                    refreshTimes = 0
                    tryReconnectTimes = 0
                    reconnectImmediatelyCount = 0
                    device.connectionState = IConnection.STATE_SERVICE_DISCOVERED
                    sendConnectionCallback()
                }
            } else {
                doClearTaskAndRefresh()
                Ble.println(javaClass, Log.ERROR, "GATT error! [status: $status, name: ${device.name}, addr: ${device.addr}]")
            }
        }
    }

    private fun doDiscoverServices() {
        if (bluetoothGatt != null) {
            bluetoothGatt!!.discoverServices()
            device.connectionState = IConnection.STATE_SERVICE_DISCOVERING
            sendConnectionCallback()
        } else {
            notifyDisconnected()
        }
    }

    private fun doTimer() {
        if (!isReleased) {
            //only process that are not connected, not refreshing and not actively disconnected
            if (device.connectionState != IConnection.STATE_SERVICE_DISCOVERED && !refreshing && !isActiveDisconnect) {
                if (device.connectionState != IConnection.STATE_DISCONNECTED) {
                    //超时
                    if (System.currentTimeMillis() - connStartTime > config.connectTimeoutMillis) {
                        connStartTime = System.currentTimeMillis()
                        Ble.println(javaClass, Log.ERROR, "connect timeout! [name: ${device.name}, addr: ${device.addr}]")
                        val type = when {
                            device.connectionState == IConnection.STATE_SCANNING -> IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE
                            device.connectionState == IConnection.STATE_CONNECTING -> IConnection.TIMEOUT_TYPE_CANNOT_CONNECT
                            else -> IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES
                        }
                        Ble.instance.postEvent(Events.newConnectTimeout(device, type))
                        stateChangeListener?.onConnectTimeout(device, type)
                        if (config.isAutoReconnect && (config.tryReconnectTimes == ConnectionConfig.TRY_RECONNECT_TIMES_INFINITE || tryReconnectTimes < config.tryReconnectTimes)) {
                            doDisconnect(true, true)
                        } else {
                            doDisconnect(false, true)
                            notifyConnectFailed(device, IConnection.CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION, stateChangeListener)
                            Ble.println(javaClass, Log.ERROR, "connect failed! [type: maximun reconnection, name: ${device.name}, addr: ${device.addr}]")
                        }
                    }
                } else if (config.isAutoReconnect) {
                    doDisconnect(true, true)
                }
            }
            connHandler.sendEmptyMessageDelayed(BaseConnection.MSG_TIMER, 500)
        }
    }

    //handle refresh
    private fun doRefresh(isAuto: Boolean) {
        Ble.println(javaClass, Log.DEBUG, "refresh GATT! [name: ${device.name}, addr: ${device.addr}]")
        connStartTime = System.currentTimeMillis() //Prevent the refresh process from automatically reconnecting
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt!!.disconnect()
            } catch (ignored: Exception) {}

            if (isAuto) {
                if (refreshTimes <= 5) {
                    refreshing = BaseConnection.refresh(bluetoothGatt!!)
                }
                refreshTimes++
            } else {
                refreshing = BaseConnection.refresh(bluetoothGatt!!)
            }
            if (refreshing) {
                connHandler.postDelayed({ cancelRefreshState() }, 2000)
            } else {
                cancelRefreshState()
            }
        }
        notifyDisconnected()
    }

    private fun cancelRefreshState() {
        if (refreshing) {
            refreshing = false
            if (bluetoothGatt != null) {
                closeGatt(bluetoothGatt)
                bluetoothGatt = null
            }
        }
    }

    private fun doConnect() {
        cancelRefreshState()
        device.connectionState = IConnection.STATE_CONNECTING
        sendConnectionCallback()
        Ble.println(javaClass, Log.DEBUG, "connecting [name: ${device.name}, addr: ${device.addr}]")
        //Scanning must be stopped when connecting, otherwise unexpected problems will arise.
        Ble.instance.stopScan()
        connHandler.postDelayed({
            if (!isReleased) {
                bluetoothGatt = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        bluetoothDevice.connectGatt(Ble.instance.context, false, this@Connection, config.transport, config.phy)
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                        bluetoothDevice.connectGatt(Ble.instance.context, false, this@Connection, config.transport)
                    }
                    else -> bluetoothDevice.connectGatt(Ble.instance.context, false, this@Connection)
                }
            }
        }, 500)
    }

    private fun doDisconnect(reconnect: Boolean, notify: Boolean) {
        clearRequestQueueAndNotify()
        if (bluetoothGatt != null) {
            closeGatt(bluetoothGatt)
            bluetoothGatt = null
        }
        device.connectionState = IConnection.STATE_DISCONNECTED
        if (isReleased) {
            device.connectionState = IConnection.STATE_RELEASED
            super.release()
            Ble.println(javaClass, Log.DEBUG, "connection released! [name: ${device.name}, addr: ${device.addr}]")
        } else if (reconnect) {
            tryReconnectTimes++
            if (reconnectImmediatelyCount < config.reconnectImmediatelyTimes) {
                reconnectImmediatelyCount++
                connStartTime = System.currentTimeMillis()
                doConnect()
            } else {
                tryScanReconnect()
            }
        }
        if (notify) {
            sendConnectionCallback()
        }
    }

    private fun closeGatt(gatt: BluetoothGatt?) {
        try {
            gatt!!.disconnect()
        } catch (ignored: Exception) {
        }

        try {
            gatt!!.close()
        } catch (ignored: Exception) {
        }

    }

    private fun tryScanReconnect() {
        if (!isReleased) {
            connStartTime = System.currentTimeMillis()
            Ble.instance.stopScan()
            connHandler.postDelayed({
                if (!isReleased) {
                    //Scan for devices before connecting
                    device.connectionState = IConnection.STATE_SCANNING
                    Ble.println(javaClass, Log.DEBUG, "scanning [name: ${device.name}, addr: ${device.addr}]")
                    Ble.instance.startScan()
                }
            }, 2000)
        }
    }

    private fun doClearTaskAndRefresh() {
        clearRequestQueueAndNotify()
        doRefresh(true)
    }

    private fun sendConnectionCallback() {
        if (lastConnectState != device.connectionState) {
            lastConnectState = device.connectionState
            stateChangeListener?.onConnectionStateChanged(device)
            Ble.instance.postEvent(Events.newConnectionStateChanged(device, device.connectionState))
        }
    }

    internal fun setAutoReconnectEnable(enable: Boolean) {
        config.setAutoReconnect(enable)
    }

    fun reconnect() {
        if (!isReleased) {
            isActiveDisconnect = false
            tryReconnectTimes = 0
            reconnectImmediatelyCount = 0
            Message.obtain(connHandler, BaseConnection.MSG_DISCONNECT, MSG_ARG_RECONNECT, 0).sendToTarget()
        }
    }

    fun disconnect() {
        if (!isReleased) {
            isActiveDisconnect = true
            Message.obtain(connHandler, BaseConnection.MSG_DISCONNECT, MSG_ARG_NONE, 0).sendToTarget()
        }
    }

    /**
     * Clears the internal cache and forces a refresh of the services from the remote device.
     */
    fun refresh() {
        connHandler.sendEmptyMessage(BaseConnection.MSG_REFRESH)
    }

    /**
     * Destroy the connection and stop timer.
     */
    override fun release() {
        super.release()
        Message.obtain(connHandler, BaseConnection.MSG_RELEASE, MSG_ARG_NOTIFY, 0).sendToTarget()
    }

    /**
     * Destroy the connection and stop timer without notify.
     */
    fun releaseNoEvnet() {
        super.release()
        Message.obtain(connHandler, BaseConnection.MSG_RELEASE, MSG_ARG_NONE, 0).sendToTarget()
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        if (!isReleased) {
            connHandler.sendMessage(Message.obtain(connHandler, BaseConnection.MSG_ON_CONNECTION_STATE_CHANGE, status, newState))
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        if (!isReleased) {
            connHandler.sendMessage(Message.obtain(connHandler, BaseConnection.MSG_ON_SERVICES_DISCOVERED, status, 0))
        }
    }

    private fun getHex(value: ByteArray): String {
        return BleUtils.bytesToHexString(value).trim { it <= ' ' }
    }

    override fun onCharacteristicRead(requestId: String, characteristic: BluetoothGattCharacteristic) {
        Ble.instance.postEvent(Events.newCharacteristicRead(device, requestId, GattCharacteristic(characteristic.service.uuid, characteristic.uuid, characteristic.value)))
        Ble.println(javaClass, Log.DEBUG, "characteristic read! [addr: ${device.addr}, value: ${getHex(characteristic.value)}]")
    }

    override fun onCharacteristicChanged(characteristic: BluetoothGattCharacteristic) {
        Ble.instance.postEvent(Events.newCharacteristicChanged(device, GattCharacteristic(characteristic.service.uuid, characteristic.uuid, characteristic.value)))
        Ble.println(javaClass, Log.INFO, "characteristic change! [addr: ${device.addr}, value: ${getHex(characteristic.value)}]")
    }

    override fun onReadRemoteRssi(requestId: String, rssi: Int) {
        Ble.instance.postEvent(Events.newRemoteRssiRead(device, requestId, rssi))
        Ble.println(javaClass, Log.DEBUG, "rssi read! [addr: ${device.addr}, rssi: $rssi]")
    }

    override fun onMtuChanged(requestId: String, mtu: Int) {
        Ble.instance.postEvent(Events.newMtuChanged(device, requestId, mtu))
        Ble.println(javaClass, Log.DEBUG, "mtu change! [addr: ${device.addr}, mtu: $mtu]")
    }

    override fun onRequestFialed(requestId: String, requestType: Request.RequestType, failType: Int, value: ByteArray?) {
        Ble.instance.postEvent(Events.newRequestFailed(device, requestId, requestType, failType, value!!))
        Ble.println(javaClass, Log.DEBUG, "request failed! [addr: ${device.addr}, requestId: $requestId, failType: $failType]")
    }

    override fun onDescriptorRead(requestId: String, descriptor: BluetoothGattDescriptor) {
        val charac = descriptor.characteristic
        Ble.instance.postEvent(Events.newDescriptorRead(device, requestId, GattDescriptor(charac.service.uuid, charac.uuid, descriptor.uuid, descriptor.value)))
        Ble.println(javaClass, Log.DEBUG, "descriptor read! [addr: ${device.addr}, value: ${getHex(descriptor.value)}]")
    }

    override fun onNotificationChanged(requestId: String, descriptor: BluetoothGattDescriptor, isEnabled: Boolean) {
        val charac = descriptor.characteristic
        Ble.instance.postEvent(Events.newNotificationChanged(device, requestId, GattDescriptor(charac.service.uuid, charac.uuid, descriptor.uuid, descriptor.value), isEnabled))
        Ble.println(javaClass, Log.DEBUG, "${if (isEnabled) "notification enabled!" else "notification disabled!"} [addr: ${device.addr}]")
    }

    override fun onIndicationChanged(requestId: String, descriptor: BluetoothGattDescriptor, isEnabled: Boolean) {
        val characteristic = descriptor.characteristic
        Ble.instance.postEvent(Events.newIndicationChanged(device, requestId, GattDescriptor(characteristic.service.uuid, characteristic.uuid, descriptor.uuid, descriptor.value), isEnabled))
        Ble.println(javaClass, Log.DEBUG, "${if (isEnabled) "indication enabled!" else "indication disabled"} [addr: ${device.addr}]")
    }

    override fun onCharacteristicWrite(requestId: String, characteristic: GattCharacteristic) {
        Ble.instance.postEvent(Events.newCharacteristicWrite(device, requestId, characteristic))
        Ble.println(javaClass, Log.DEBUG, "write success! [addr: ${device.addr}, value: ${getHex(characteristic.value)}]")
    }

    override fun onPhyReadOrUpdate(requestId: String, read: Boolean, txPhy: Int, rxPhy: Int) {
        val event = if (read) {
            Ble.println(javaClass, Log.DEBUG, "phy read! [addr: ${device.addr}, tvPhy: $txPhy, rxPhy: $rxPhy]")
            Events.newPhyRead(device, requestId, txPhy, rxPhy)
        } else {
            Ble.println(javaClass, Log.DEBUG, "phy update! [addr: ${device.addr}, tvPhy: $txPhy, rxPhy: $rxPhy]")
            Events.newPhyUpdate(device, requestId, txPhy, rxPhy)
        }
        Ble.instance.postEvent(event)        
    }

    companion object {
        private const val MSG_ARG_NONE = 0
        private const val MSG_ARG_RECONNECT = 1
        private const val MSG_ARG_NOTIFY = 2

        /**
         * Create a new connection.
         */
        @Synchronized
        internal fun newInstance(bluetoothAdapter: BluetoothAdapter, device: Device, config: ConnectionConfig?, connectDelay: Long, 
                                 stateChangeListener: ConnectionStateChangeListener?): Connection? {
            var connectionConfig = config
            if (!device.addr.matches("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$".toRegex())) {
                Ble.println(Connection::class.java, Log.ERROR, "connect failed! [type: unspecified mac address, name: ${device.name}, addr: ${device.addr}]")
                notifyConnectFailed(device, IConnection.CONNECT_FAIL_TYPE_UNSPECIFIED_ADDRESS, stateChangeListener)
                return null
            }
            if (connectionConfig == null) {
                connectionConfig = ConnectionConfig()
            }
            val conn = Connection(device, bluetoothAdapter.getRemoteDevice(device.addr), connectionConfig)
            conn.bluetoothAdapter = bluetoothAdapter
            conn.stateChangeListener = stateChangeListener
            conn.connStartTime = System.currentTimeMillis()
            conn.connHandler.sendEmptyMessageDelayed(BaseConnection.MSG_CONNECT, connectDelay) //do connect
            conn.connHandler.sendEmptyMessageDelayed(BaseConnection.MSG_TIMER, connectDelay) //start timer
            return conn
        }

        internal fun notifyConnectFailed(device: Device?, type: Int, listener: ConnectionStateChangeListener?) {
            listener?.onConnectFailed(device, type)
            Ble.instance.postEvent(Events.newConnectFailed(device, type))
        }
    }
}
