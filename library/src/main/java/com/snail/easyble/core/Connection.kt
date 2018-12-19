package com.snail.easyble.core

import android.bluetooth.*
import android.os.Build
import android.os.Message
import android.util.Log
import com.snail.easyble.callback.ConnectionStateChangeListener
import com.snail.easyble.event.Events
import com.snail.easyble.util.BleUtils

/**
 * 描述: 蓝牙连接
 * 时间: 2018/4/11 15:29
 * 作者: zengfansheng
 */
class Connection private constructor(device: Device, bluetoothDevice: BluetoothDevice, config: ConnectionConfig) : BaseConnection(device, bluetoothDevice, config) {

    private var stateChangeListener: ConnectionStateChangeListener? = null
    private var connStartTime: Long = 0
    private var refreshTimes: Int = 0 //记录刷新次数，如果成功发现服务器，则清零
    private var tryReconnectTimes: Int = 0 //尝试重连次数
    private var lastConnectState = -1
    private var reconnectImmediatelyCount: Int = 0 //不搜索，直接连接次数
    private var refreshing: Boolean = false
    private var isActiveDisconnect: Boolean = false

    internal val isAutoReconnectEnabled: Boolean
        get() = config.isAutoReconnect

    val connctionState: Int
        get() = device!!.connectionState

    @Synchronized
    internal fun onScanResult(addr: String) {
        if (!isReleased && device!!.addr == addr && device!!.connectionState == IConnection.STATE_SCANNING) {
            connHandler.sendEmptyMessage(BaseConnection.MSG_CONNECT)
        }
    }

    override fun handleMsg(msg: Message) {
        if (isReleased && msg.what != BaseConnection.MSG_RELEASE) {
            return
        }
        when (msg.what) {
            BaseConnection.MSG_CONNECT //连接
            -> if (bluetoothAdapter!!.isEnabled) {
                doConnect()
            }
            BaseConnection.MSG_DISCONNECT //处理断开
            -> doDisconnect(msg.arg1 == MSG_ARG_RECONNECT && bluetoothAdapter!!.isEnabled, true)
            BaseConnection.MSG_REFRESH //手动刷新
            -> doRefresh(false)
            BaseConnection.MSG_AUTO_REFRESH //自动刷新
            -> doRefresh(true)
            BaseConnection.MSG_RELEASE //销毁连接
            -> {
                config.setAutoReconnect(false) //停止重连
                doDisconnect(false, msg.arg1 == MSG_ARG_NOTIFY)
            }
            BaseConnection.MSG_TIMER //定时器
            -> doTimer()
            BaseConnection.MSG_DISCOVER_SERVICES, //开始发现服务
            BaseConnection.MSG_ON_CONNECTION_STATE_CHANGE, //连接状态变化
            BaseConnection.MSG_ON_SERVICES_DISCOVERED //发现服务
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
        device!!.connectionState = IConnection.STATE_DISCONNECTED
        sendConnectionCallback()
    }

    private fun doOnConnectionStateChange(status: Int, newState: Int) {
        if (bluetoothGatt != null) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Ble.println(javaClass, Log.DEBUG, "connected! [name: ${device!!.name}, mac: ${device!!.addr}]")
                    device!!.connectionState = IConnection.STATE_CONNECTED
                    sendConnectionCallback()
                    // 进行服务发现，延时
                    connHandler.sendEmptyMessageDelayed(BaseConnection.MSG_DISCOVER_SERVICES, config.discoverServicesDelayMillis)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Ble.println(javaClass, Log.DEBUG, "disconnected! [name: ${device!!.name}, mac: ${device!!.addr}, autoReconnEnable: ${config.isAutoReconnect}]")
                    clearRequestQueueAndNotify()
                    notifyDisconnected()
                }
            } else {
                Ble.println(javaClass, Log.ERROR, "GATT error! [name: ${device!!.name}, mac: ${device!!.addr}, status: $status]")
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
                Ble.println(javaClass, Log.DEBUG, "services discovered! [name: ${device!!.name}, mac: ${device!!.addr}, size: ${bluetoothGatt!!.services.size}]")
                if (services.isEmpty()) {
                    doClearTaskAndRefresh()
                } else {
                    refreshTimes = 0
                    tryReconnectTimes = 0
                    reconnectImmediatelyCount = 0
                    device!!.connectionState = IConnection.STATE_SERVICE_DISCOVERED
                    sendConnectionCallback()
                }
            } else {
                doClearTaskAndRefresh()
                Ble.println(javaClass, Log.ERROR, "GATT error! [status: $status, name: ${device!!.name}, mac: ${device!!.addr}]")
            }
        }
    }

    private fun doDiscoverServices() {
        if (bluetoothGatt != null) {
            bluetoothGatt!!.discoverServices()
            device!!.connectionState = IConnection.STATE_SERVICE_DISCOVERING
            sendConnectionCallback()
        } else {
            notifyDisconnected()
        }
    }

    private fun doTimer() {
        if (!isReleased) {
            //只处理不在连接状态的、不在刷新、不是主动断开
            if (device!!.connectionState != IConnection.STATE_SERVICE_DISCOVERED && !refreshing && !isActiveDisconnect) {
                if (device!!.connectionState != IConnection.STATE_DISCONNECTED) {
                    //超时
                    if (System.currentTimeMillis() - connStartTime > config.connectTimeoutMillis) {
                        connStartTime = System.currentTimeMillis()
                        Ble.println(javaClass, Log.ERROR, "connect timeout! [name: ${device!!.name}, mac: ${device!!.addr}]")
                        val type = when {
                            device!!.connectionState == IConnection.STATE_SCANNING -> IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE
                            device!!.connectionState == IConnection.STATE_CONNECTING -> IConnection.TIMEOUT_TYPE_CANNOT_CONNECT
                            else -> IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES
                        }
                        Ble.instance.postEvent(Events.newConnectTimeout(device!!, type))
                        stateChangeListener?.onConnectTimeout(device!!, type)
                        if (config.isAutoReconnect && (config.tryReconnectTimes == ConnectionConfig.TRY_RECONNECT_TIMES_INFINITE || tryReconnectTimes < config.tryReconnectTimes)) {
                            doDisconnect(true, true)
                        } else {
                            doDisconnect(false, true)
                            notifyConnectFailed(device, IConnection.CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION, stateChangeListener)
                            Ble.println(javaClass, Log.ERROR, "connect failed! [type: maximun reconnection, name: ${device!!.name}, mac: ${device!!.addr}]")
                        }
                    }
                } else if (config.isAutoReconnect) {
                    doDisconnect(true, true)
                }
            }
            connHandler.sendEmptyMessageDelayed(BaseConnection.MSG_TIMER, 500)
        }
    }

    //处理刷新
    private fun doRefresh(isAuto: Boolean) {
        Ble.println(javaClass, Log.DEBUG, "refresh GATT! [name: ${device!!.name}, mac: ${device!!.addr}]")
        connStartTime = System.currentTimeMillis() //防止刷新过程自动重连
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
        device!!.connectionState = IConnection.STATE_CONNECTING
        sendConnectionCallback()
        Ble.println(javaClass, Log.DEBUG, "connecting [name: ${device!!.name}, mac: ${device!!.addr}]")
        //连接时需要停止蓝牙扫描
        Ble.instance.stopScan()
        connHandler.postDelayed({
            if (!isReleased) {
                bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    bluetoothDevice.connectGatt(Ble.instance.context, false, this@Connection,
                            if (config.transport == -1) BluetoothDevice.TRANSPORT_AUTO else config.transport)
                } else {
                    bluetoothDevice.connectGatt(Ble.instance.context, false, this@Connection)
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
        device!!.connectionState = IConnection.STATE_DISCONNECTED
        if (isReleased) { //销毁
            device!!.connectionState = IConnection.STATE_RELEASED
            super.release()
            Ble.println(javaClass, Log.DEBUG, "connection released! [name: ${device!!.name}, mac: ${device!!.addr}]")
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
                    //开启扫描，扫描到才连接
                    device!!.connectionState = IConnection.STATE_SCANNING
                    Ble.println(javaClass, Log.DEBUG, "scanning [name: ${device!!.name}, mac: ${device!!.addr}]")
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
        if (lastConnectState != device!!.connectionState) {
            lastConnectState = device!!.connectionState
            stateChangeListener?.onConnectionStateChanged(device!!)
            Ble.instance.postEvent(Events.newConnectionStateChanged(device!!, device!!.connectionState))
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
     * 清理缓存
     */
    fun refresh() {
        connHandler.sendEmptyMessage(BaseConnection.MSG_REFRESH)
    }

    /**
     * 销毁连接，停止定时器
     */
    override fun release() {
        super.release()
        Message.obtain(connHandler, BaseConnection.MSG_RELEASE, MSG_ARG_NOTIFY, 0).sendToTarget()
    }

    /**
     * 销毁连接，不发布消息
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
        Ble.instance.postEvent(Events.newCharacteristicRead(device!!, requestId, GattCharacteristic(characteristic.service.uuid, characteristic.uuid, characteristic.value)))
        Ble.println(javaClass, Log.DEBUG, "characteristic read! [mac: ${device!!.addr}, value: ${getHex(characteristic.value)}]")
    }

    override fun onCharacteristicChanged(characteristic: BluetoothGattCharacteristic) {
        Ble.instance.postEvent(Events.newCharacteristicChanged(device!!, GattCharacteristic(characteristic.service.uuid, characteristic.uuid, characteristic.value)))
        Ble.println(javaClass, Log.INFO, "characteristic change! [mac: ${device!!.addr}, value: ${getHex(characteristic.value)}]")
    }

    override fun onReadRemoteRssi(requestId: String, rssi: Int) {
        Ble.instance.postEvent(Events.newRemoteRssiRead(device!!, requestId, rssi))
        Ble.println(javaClass, Log.DEBUG, "rssi read! [mac: ${device!!.addr}, rssi: $rssi]")
    }

    override fun onMtuChanged(requestId: String, mtu: Int) {
        Ble.instance.postEvent(Events.newMtuChanged(device!!, requestId, mtu))
        Ble.println(javaClass, Log.DEBUG, "mtu change! [mac: ${device!!.addr}, mtu: $mtu]")
    }

    override fun onRequestFialed(requestId: String, requestType: Request.RequestType, failType: Int, value: ByteArray?) {
        Ble.instance.postEvent(Events.newRequestFailed(device!!, requestId, requestType, failType, value!!))
        Ble.println(javaClass, Log.DEBUG, "request failed! [mac: ${device!!.addr}, requestId: $requestId, failType: $failType]")
    }

    override fun onDescriptorRead(requestId: String, descriptor: BluetoothGattDescriptor) {
        val charac = descriptor.characteristic
        Ble.instance.postEvent(Events.newDescriptorRead(device!!, requestId, GattDescriptor(charac.service.uuid, charac.uuid, descriptor.uuid, descriptor.value)))
        Ble.println(javaClass, Log.DEBUG, "descriptor read! [mac: ${device!!.addr}, value: ${getHex(descriptor.value)}]")
    }

    override fun onNotificationChanged(requestId: String, descriptor: BluetoothGattDescriptor, isEnabled: Boolean) {
        val charac = descriptor.characteristic
        Ble.instance.postEvent(Events.newNotificationChanged(device!!, requestId, GattDescriptor(charac.service.uuid, charac.uuid, descriptor.uuid, descriptor.value), isEnabled))
        Ble.println(javaClass, Log.DEBUG, "${if (isEnabled) "notification enabled!" else "notification disabled!"} [mac: ${device!!.addr}]")
    }

    override fun onIndicationChanged(requestId: String, descriptor: BluetoothGattDescriptor, isEnabled: Boolean) {
        val characteristic = descriptor.characteristic
        Ble.instance.postEvent(Events.newIndicationChanged(device!!, requestId, GattDescriptor(characteristic.service.uuid, characteristic.uuid, descriptor.uuid, descriptor.value), isEnabled))
        Ble.println(javaClass, Log.DEBUG, "${if (isEnabled) "indication enabled!" else "indication disabled"} [mac: ${device!!.addr}]")
    }

    override fun onCharacteristicWrite(requestId: String, characteristic: GattCharacteristic) {
        Ble.instance.postEvent(Events.newCharacteristicWrite(device!!, requestId, characteristic))
        Ble.println(javaClass, Log.DEBUG, "write success! [mac: ${device!!.addr}, value: ${getHex(characteristic.value)}]")
    }

    companion object {
        private const val MSG_ARG_NONE = 0
        private const val MSG_ARG_RECONNECT = 1
        private const val MSG_ARG_NOTIFY = 2

        /**
         * 连接
         */
        @Synchronized
        internal fun newInstance(bluetoothAdapter: BluetoothAdapter, device: Device, config: ConnectionConfig?, connectDelay: Long, 
                                 stateChangeListener: ConnectionStateChangeListener?): Connection? {
            var connectionConfig = config
            if (!device.addr.matches("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$".toRegex())) {
                Ble.println(Connection::class.java, Log.ERROR, "connect failed! [type: unspecified mac address, name: ${device.name}, mac: ${device.addr}]")
                notifyConnectFailed(device, IConnection.CONNECT_FAIL_TYPE_UNSPECIFIED_MAC_ADDRESS, stateChangeListener)
                return null
            }
            //初始化并建立连接
            if (connectionConfig == null) {
                connectionConfig = ConnectionConfig()
            }
            val conn = Connection(device, bluetoothAdapter.getRemoteDevice(device.addr), connectionConfig)
            conn.bluetoothAdapter = bluetoothAdapter
            conn.stateChangeListener = stateChangeListener
            //连接蓝牙设备
            conn.connStartTime = System.currentTimeMillis()
            conn.connHandler.sendEmptyMessageDelayed(BaseConnection.MSG_CONNECT, connectDelay) //连接
            conn.connHandler.sendEmptyMessageDelayed(BaseConnection.MSG_TIMER, connectDelay) //启动定时器，用于断线重连
            return conn
        }

        internal fun notifyConnectFailed(device: Device?, type: Int, listener: ConnectionStateChangeListener?) {
            listener?.onConnectFailed(device!!, type)
            Ble.instance.postEvent(Events.newConnectFailed(device!!, type))
        }
    }
}
