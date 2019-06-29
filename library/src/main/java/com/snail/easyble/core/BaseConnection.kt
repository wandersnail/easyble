package com.snail.easyble.core

import android.bluetooth.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import com.snail.easyble.callback.*
import com.snail.easyble.util.BleUtils
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * date: 2018/4/11 16:37
 * author: zengfansheng
 */
@Suppress("LeakingThis")
abstract class BaseConnection internal constructor(val device: Device, protected var bluetoothDevice: BluetoothDevice, val config: ConnectionConfig) : 
        BluetoothGattCallback(), IConnection {
    var bluetoothGatt: BluetoothGatt? = null
        protected set
    private var requestQueue: MutableList<Request> = ArrayList()
    private var currentRequest: Request? = null
    private var pendingCharacteristic: BluetoothGattCharacteristic? = null
    protected var bluetoothAdapter: BluetoothAdapter? = null
    protected var isReleased: Boolean = false
    internal var connHandler: Handler
    private var characteristicChangedCallback: CharacteristicChangedCallback? = null

    init {
        connHandler = ConnHandler(this)
    }

    /**
     * 返回一个蓝牙服务列表，只有在执行了发现服务后，才会有
     */
    val gattServices: List<BluetoothGattService>
        get() = if (bluetoothGatt != null) {
            bluetoothGatt!!.services
        } else ArrayList()

    /**
     * 清除请求队列，不触发事件
     */
    fun clearRequestQueue() {
        synchronized(this) {
            requestQueue.clear()
            currentRequest = null
        }
    }

    /**
     * 设置收到特征通知数据回调，作用于有nofity属性的特征
     */
    fun setCharacteristicChangedCallback(characteristicChangedCallback: CharacteristicChangedCallback) {
        this.characteristicChangedCallback = characteristicChangedCallback
    }

    /**
     * 将指定的请求类型从队列中移除，不触发事件
     */
    fun clearRequestQueueByType(type: Request.RequestType) {
        synchronized(this) {
            val it = requestQueue.iterator()
            while (it.hasNext()) {
                val request = it.next()
                if (request.type == type) {
                    it.remove()
                }
            }
            if (currentRequest != null && currentRequest!!.type == type) {
                currentRequest = null
            }
        }
    }

    /**
     * 清空请求队列并触发通知事件
     */
    internal fun clearRequestQueueAndNotify() {
        synchronized(this) {
            for (request in requestQueue) {
                handleFailedCallback(request, IConnection.REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED, false)
            }
            if (currentRequest != null) {
                handleFailedCallback(currentRequest!!, IConnection.REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED, false)
            }
        }
        clearRequestQueue()
    }

    open fun release() {
        isReleased = true
        connHandler.removeCallbacksAndMessages(null)
        clearRequestQueueAndNotify()
    }

    fun getService(serviceUuid: UUID): BluetoothGattService? {
        return if (bluetoothGatt != null) {
            bluetoothGatt!!.getService(serviceUuid)
        } else null
    }

    fun getCharacteristic(serviceUuid: UUID, characteristicUuid: UUID): BluetoothGattCharacteristic? {
        if (bluetoothGatt != null) {
            val service = bluetoothGatt!!.getService(serviceUuid)
            if (service != null) {
                return service.getCharacteristic(characteristicUuid)
            }
        }
        return null
    }

    fun getDescriptor(serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID): BluetoothGattDescriptor? {
        if (bluetoothGatt != null) {
            val service = bluetoothGatt!!.getService(serviceUuid)
            if (service != null) {
                val characteristic = service.getCharacteristic(characteristicUuid)
                if (characteristic != null) {
                    return characteristic.getDescriptor(descriptorUuid)
                }
            }
        }
        return null
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        if (currentRequest != null) {
            if (currentRequest!!.type == Request.RequestType.READ_CHARACTERISTIC) {
                val request = currentRequest!!
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (request.callback != null) {
                        Ble.instance.getMethodPoster().post(request.callback, CharacteristicReadCallback.getMethodInfo(device, request.tag, 
                                characteristic.service.uuid, characteristic.uuid, characteristic.value))
                    } else {
                        onCharacteristicRead(request.tag, characteristic)
                    }
                } else {
                    handleGattStatusFailed()
                }
                executeNextRequest()
            }
        }
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        if (currentRequest != null && currentRequest!!.waitWriteResult && currentRequest!!.type == Request.RequestType.WRITE_CHARACTERISTIC) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (currentRequest!!.remainQueue == null || currentRequest!!.remainQueue!!.isEmpty()) {
                    val request = currentRequest!!
                    if (request.callback != null) {
                        Ble.instance.getMethodPoster().post(request.callback, CharacteristicWriteCallback.getMethodInfo(device, request.tag, 
                                characteristic.service.uuid, characteristic.uuid, request.value!!))
                    } else {
                        onCharacteristicWrite(request.tag, characteristic.service.uuid, characteristic.uuid, request.value!!)
                    }
                    executeNextRequest()
                } else {
                    connHandler.removeMessages(MSG_REQUEST_TIMEOUT)
                    connHandler.sendMessageDelayed(Message.obtain(connHandler, MSG_REQUEST_TIMEOUT, currentRequest), config.requestTimeoutMillis.toLong())
                    val delay = currentRequest!!.writeDelay.toLong()
                    if (delay > 0) {
                        val req = currentRequest
                        Thread.sleep(delay)
                        if (req != currentRequest) {
                            return
                        }
                    }
                    currentRequest!!.sendingBytes = currentRequest!!.remainQueue!!.remove()
                    if (writeFail(characteristic, currentRequest!!.sendingBytes!!)) {
                        handleWriteFailed(currentRequest!!)
                    }
                }
            } else {
                handleFailedCallback(currentRequest!!, IConnection.REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, true)
            }
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        onCharacteristicChanged(characteristic)
        if (characteristicChangedCallback != null) {
            Ble.instance.getMethodPoster().post(characteristicChangedCallback!!, CharacteristicChangedCallback.getMethodInfo(device, 
                    characteristic.service.uuid, characteristic.uuid, characteristic.value))
        }
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        if (currentRequest != null) {
            if (currentRequest!!.type == Request.RequestType.READ_RSSI) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val request = currentRequest!!
                    if (request.callback != null) {
                        Ble.instance.getMethodPoster().post(request.callback, RemoteRssiReadCallback.getMethodInfo(device, request.tag, rssi))
                    } else {
                        onReadRemoteRssi(request.tag, rssi)
                    }
                } else {
                    handleGattStatusFailed()
                }
                executeNextRequest()
            }
        }
    }

    override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        if (currentRequest != null) {
            val characteristic = descriptor.characteristic
            when (currentRequest!!.type) {
                Request.RequestType.ENABLE_NOTIFICATION,
                Request.RequestType.DISABLE_NOTIFICATION,
                Request.RequestType.ENABLE_INDICATION,
                Request.RequestType.DISABLE_INDICATION -> {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        handleGattStatusFailed()
                    } else if (characteristic.service.uuid == pendingCharacteristic!!.service.uuid && characteristic.uuid == pendingCharacteristic!!.uuid) {
                        val isEnableNotify = currentRequest!!.type == Request.RequestType.ENABLE_NOTIFICATION
                        if (enableNotificationOrIndicationFail(isEnableNotify || currentRequest!!.type == Request.RequestType.ENABLE_INDICATION,
                                        isEnableNotify, characteristic)) {
                            handleGattStatusFailed()
                        }
                    }
                }
                Request.RequestType.READ_DESCRIPTOR -> {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val request = currentRequest!!
                        if (request.callback != null) {
                            Ble.instance.getMethodPoster().post(request.callback, DescriptorReadCallback.getMethodInfo(device, request.tag, 
                                    characteristic.service.uuid, characteristic.uuid, descriptor.uuid, descriptor.value))
                        } else {
                            onDescriptorRead(request.tag, descriptor)
                        }
                    } else {
                        handleGattStatusFailed()
                    }
                    executeNextRequest()
                }
                else -> {
                }
            }
        }
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor, status: Int) {
        if (currentRequest != null) {
            if (currentRequest!!.type == Request.RequestType.ENABLE_NOTIFICATION || currentRequest!!.type == Request.RequestType.DISABLE_NOTIFICATION ||
                    currentRequest!!.type == Request.RequestType.ENABLE_INDICATION || currentRequest!!.type == Request.RequestType.DISABLE_INDICATION) {
                val localDescriptor = getDescriptor(descriptor.characteristic.service.uuid, descriptor.characteristic.uuid, IConnection.clientCharacteristicConfig)
                val request = currentRequest!!
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleGattStatusFailed()
                    localDescriptor?.value = currentRequest!!.value
                } else {
                    val isEnabled = currentRequest!!.type == Request.RequestType.ENABLE_NOTIFICATION || currentRequest!!.type == Request.RequestType.ENABLE_INDICATION
                    if (request.callback != null) {
                        val ch = descriptor.characteristic
                        if (request.type == Request.RequestType.ENABLE_NOTIFICATION || request.type == Request.RequestType.DISABLE_NOTIFICATION) {
                            Ble.instance.getMethodPoster().post(request.callback, NotificationChangedCallback.getMethodInfo(device, 
                                    request.tag, ch.service.uuid, ch.uuid, descriptor.uuid, isEnabled))
                        } else {
                            Ble.instance.getMethodPoster().post(request.callback, IndicationChangedCallback.getMethodInfo(device,
                                    request.tag, ch.service.uuid, ch.uuid, descriptor.uuid, isEnabled))
                        }
                    } else if (request.type == Request.RequestType.ENABLE_NOTIFICATION || request.type == Request.RequestType.DISABLE_NOTIFICATION) {
                        onNotificationChanged(request.tag, descriptor, isEnabled)
                    } else {
                        onIndicationChanged(request.tag, descriptor, isEnabled)
                    }
                }
                executeNextRequest()
            }
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        if (currentRequest != null) {
            if (currentRequest!!.type == Request.RequestType.CHANGE_MTU) {
                val request = currentRequest!!
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (request.callback != null) {
                        Ble.instance.getMethodPoster().post(request.callback, MtuChangedCallback.getMethodInfo(device, request.tag, mtu))
                    } else {
                        onMtuChanged(request.tag, mtu)
                    }
                } else {
                    handleGattStatusFailed()
                }
                executeNextRequest()
            }
        }
    }

    override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
        handlePhyReadOrUpdate(true, txPhy, rxPhy, status)
    }

    override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
        handlePhyReadOrUpdate(false, txPhy, rxPhy, status)
    }

    private fun handlePhyReadOrUpdate(read: Boolean, txPhy: Int, rxPhy: Int, status: Int) {
        if (currentRequest != null) {
            if ((read && currentRequest!!.type == Request.RequestType.READ_PHY) || ((!read && currentRequest!!.type == Request.RequestType.SET_PREFERRED_PHY))) {
                val request = currentRequest!!
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (request.callback != null) {
                        if (read) {
                            Ble.instance.getMethodPoster().post(request.callback, PhyReadCallback.getMethodInfo(device, request.tag, txPhy, rxPhy))
                        } else {
                            Ble.instance.getMethodPoster().post(request.callback, PhyUpdateCallback.getMethodInfo(device, request.tag, txPhy, rxPhy))
                        }
                    } else {
                        onPhyReadOrUpdate(request.tag, read, txPhy, rxPhy)
                    }
                } else {
                    handleGattStatusFailed()
                }
                executeNextRequest()
            }
        }
    }

    //make sure currentRequest is not null
    private fun handleGattStatusFailed() {
        if (currentRequest != null) {
            handleFailedCallback(currentRequest!!, IConnection.REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, false)
        }
    }

    private fun handleFailedCallback(tag: String, requestType: Request.RequestType, failType: Int, value: ByteArray?, executeNext: Boolean) {
        onRequestFialed(tag, requestType, failType, value)
        if (executeNext) {
            executeNextRequest()
        }
    }

    private fun handleFailedCallback(request: Request, failType: Int, executeNext: Boolean) {
        if (request.callback != null) {
            if (request.callback is RequestFailedCallback) {
                handleFailedCallback(request.callback, device, request.tag, request.type, failType, request.value)
            }
        } else {
            onRequestFialed(request.tag, request.type, failType, request.value)
        }
        if (executeNext) {
            executeNextRequest()
        }
    }

    private fun handleFailedCallback(callback: RequestFailedCallback, device: Device, tag: String, requestType: Request.RequestType, failType: Int, src: ByteArray?) {
        Ble.instance.getMethodPoster().post(callback, RequestFailedCallback.getMethodInfo(device, tag, requestType, failType, src))
    }

    /**
     * 修改最大传输单元
     *
     * @param tag 用于区分请求
     * @param mtu 最大传输单元
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @JvmOverloads
    fun changeMtu(tag: String, @IntRange(from = 23, to = 517) mtu: Int, callback: MtuChangedCallback? = null, priority: Int = 0) {
        enqueue(Request.newChangeMtuRequest(tag, mtu, callback, priority))
    }

    /**
     * 读取蓝牙设备的特征，只有属性有read的才能成功
     */
    @JvmOverloads
    fun readCharacteristic(tag: String, service: UUID, characteristic: UUID, callback: CharacteristicReadCallback? = null, priority: Int = 0) {
        if (checkUuidExists(tag, Request.RequestType.READ_CHARACTERISTIC, null, callback, service, characteristic)) {
            enqueue(Request.newReadCharacteristicRequest(tag, service, characteristic, callback, priority))
        }
    }

    /**
     * 开启数据通知，特征属性中需要有notify
     */
    @JvmOverloads
    fun enableNotification(tag: String, service: UUID, characteristic: UUID, callback: NotificationChangedCallback? = null, priority: Int = 0) {
        if (checkUuidExists(tag, Request.RequestType.ENABLE_NOTIFICATION, null, callback, service, characteristic)) {
            enqueue(Request.newEnableNotificationRequest(tag, service, characteristic, callback, priority))
        }
    }

    @JvmOverloads
    fun disableNotification(tag: String, service: UUID, characteristic: UUID, callback: NotificationChangedCallback? = null, priority: Int = 0) {
        if (checkUuidExists(tag, Request.RequestType.DISABLE_NOTIFICATION, null, callback, service, characteristic)) {
            enqueue(Request.newDisableNotificationRequest(tag, service, characteristic, callback, priority))
        }
    }

    @JvmOverloads
    fun enableIndication(tag: String, service: UUID, characteristic: UUID, callback: IndicationChangedCallback? = null, priority: Int = 0) {
        if (checkUuidExists(tag, Request.RequestType.ENABLE_INDICATION, null, callback, service, characteristic)) {
            enqueue(Request.newEnableIndicationRequest(tag, service, characteristic, callback, priority))
        }
    }

    @JvmOverloads
    fun disableIndication(tag: String, service: UUID, characteristic: UUID, callback: IndicationChangedCallback? = null, priority: Int = 0) {
        if (checkUuidExists(tag, Request.RequestType.DISABLE_INDICATION, null, callback, service, characteristic)) {
            enqueue(Request.newDisableIndicationRequest(tag, service, characteristic, callback, priority))
        }
    }

    /**
     * Reads the value for a given descriptor from the associated remote device.
     */
    @JvmOverloads
    fun readDescriptor(tag: String, service: UUID, characteristic: UUID, descriptor: UUID, callback: DescriptorReadCallback? = null, priority: Int = 0) {
        if (checkUuidExists(tag, Request.RequestType.READ_DESCRIPTOR, null, callback, service, characteristic)) {
            enqueue(Request.newReadDescriptorRequest(tag, service, characteristic, descriptor, callback, priority))
        }
    }

    /**
     * Writes a given characteristic and its values to the associated remote device.
     */
    @JvmOverloads
    fun writeCharacteristic(tag: String, service: UUID, characteristic: UUID, value: ByteArray?, callback: CharacteristicWriteCallback? = null, priority: Int = 0) {
        if (value == null || value.isEmpty()) {
            if (callback != null) {
                handleFailedCallback(callback, device, tag, Request.RequestType.WRITE_CHARACTERISTIC, IConnection.REQUEST_FAIL_TYPE_VALUE_IS_NULL_OR_EMPTY, value)
            } else {
                handleFailedCallback(tag, Request.RequestType.WRITE_CHARACTERISTIC, IConnection.REQUEST_FAIL_TYPE_VALUE_IS_NULL_OR_EMPTY, value, false)
            }
            return
        } else if (checkUuidExists(tag, Request.RequestType.WRITE_CHARACTERISTIC, value, callback, service, characteristic)) {
            enqueue(Request.newWriteCharacteristicRequest(tag, service, characteristic, value, callback, priority))
        }
    }

    /**
     * 读取已连接的蓝牙设备的信号强度
     */
    @JvmOverloads
    fun readRssi(tag: String, callback: RemoteRssiReadCallback? = null, priority: Int = 0) {
        enqueue(Request.newReadRssiRequest(tag, callback, priority))
    }

    /**
     * Read the current transmitter PHY and receiver PHY of the connection. The values are returned
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @JvmOverloads
    fun readPhy(tag: String, callback: PhyReadCallback? = null, priority: Int = 0) {
        enqueue(Request.newReadPhyRequest(tag, callback, priority))
    }

    /**
     * Set the preferred connection PHY for this app. Please note that this is just a
     * recommendation, whether the PHY change will happen depends on other applications preferences,
     * local and remote controller capabilities. Controller can override these settings.
     *
     * @param txPhy preferred transmitter PHY. Bitwise OR of any of [BluetoothDevice.PHY_LE_1M_MASK], [BluetoothDevice.PHY_LE_2M_MASK], and [BluetoothDevice.PHY_LE_CODED_MASK].
     * @param rxPhy preferred receiver PHY. Bitwise OR of any of [BluetoothDevice.PHY_LE_1M_MASK], [BluetoothDevice.PHY_LE_2M_MASK], and [BluetoothDevice.PHY_LE_CODED_MASK].
     * @param phyOptions preferred coding to use when transmitting on the LE Coded PHY. Can be one
     * of [BluetoothDevice.PHY_OPTION_NO_PREFERRED], [BluetoothDevice.PHY_OPTION_S2] or
     * [BluetoothDevice.PHY_OPTION_S8]
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @JvmOverloads
    fun setPreferredPhy(tag: String, txPhy: Int, rxPhy: Int, phyOptions: Int, callback: PhyUpdateCallback? = null, priority: Int = 0) {
        enqueue(Request.newSetPreferredPhyRequest(tag, txPhy, rxPhy, phyOptions, callback, priority))
    }

    //检查uuid是否存在
    private fun checkUuidExists(tag: String, requestType: Request.RequestType, src: ByteArray?, callback: Any?, vararg uuids: UUID): Boolean {
        return when {
            uuids.isNotEmpty() -> {
                checkServiceExists(uuids[0], tag, requestType, src, callback)
            }
            uuids.size > 1 -> {
                checkCharacteristicExists(uuids[0], uuids[1], tag, requestType, src, callback)
            }
            uuids.size > 2 -> {
                checkDescriptoreExists(uuids[0], uuids[1], uuids[2], tag, requestType, src, callback)
            }
            else -> false
        }
    }

    //检查服务是否存在
    private fun checkServiceExists(uuid: UUID, tag: String, requestType: Request.RequestType, src: ByteArray?, callback: Any?): Boolean {
        return if (getService(uuid) == null) {
            if (callback == null) {
                handleFailedCallback(tag, requestType, IConnection.REQUEST_FAIL_TYPE_NULL_SERVICE, src, false)
            } else if (callback is RequestFailedCallback) {
                handleFailedCallback(callback, device, tag, requestType, IConnection.REQUEST_FAIL_TYPE_NULL_SERVICE, src)
            }
            false
        } else true
    }

    //检查特征是否存在
    private fun checkCharacteristicExists(service: UUID, characteristic: UUID, tag: String, requestType: Request.RequestType,
                                          src: ByteArray?, callback: Any?): Boolean {
        return if (checkServiceExists(service, tag, requestType, src, callback)) {
            if (getCharacteristic(service, characteristic) == null) {
                if (callback == null) {
                    handleFailedCallback(tag, requestType, IConnection.REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC, src, false)
                } else if (callback is RequestFailedCallback) {
                    handleFailedCallback(callback, device, tag, requestType, IConnection.REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC, src)
                }
                false
            } else true
        } else false
    }

    //检查Descriptore是否存在
    private fun checkDescriptoreExists(service: UUID, characteristic: UUID, descriptor: UUID, tag: String, requestType: Request.RequestType,
                                       src: ByteArray?, callback: Any?): Boolean {
        return if (checkServiceExists(service, tag, requestType, src, callback) && checkCharacteristicExists(service, characteristic,
                        tag, requestType, src, callback)) {
            if (getDescriptor(service, characteristic, descriptor) == null) {
                if (callback == null) {
                    handleFailedCallback(tag, requestType, IConnection.REQUEST_FAIL_TYPE_NULL_DESCRIPTOR, src, false)
                } else if (callback is RequestFailedCallback) {
                    handleFailedCallback(callback, device, tag, requestType, IConnection.REQUEST_FAIL_TYPE_NULL_DESCRIPTOR, src)
                }
                false
            } else true
        } else false
    }

    private fun enqueue(request: Request) {
        if (isReleased) {
            handleFailedCallback(request, IConnection.REQUEST_FAIL_TYPE_CONNECTION_RELEASED, false)
        } else {
            synchronized(this) {
                if (currentRequest == null) {
                    executeRequest(request)
                } else {
                    //根据优化级将请求插入队列中
                    var index = -1
                    run {
                        requestQueue.forEachIndexed { i, req ->
                            if (req.priority >= request.priority) {
                                if (i < requestQueue.size - 1) {
                                    if (requestQueue[i + 1].priority < request.priority) {
                                        index = i + 1
                                        return@run
                                    }
                                } else {
                                    index = i + 1
                                }
                            }
                        }
                    }
                    when {
                        index == -1 -> requestQueue.add(0, request)
                        index >= requestQueue.size -> requestQueue.add(request)
                        else -> requestQueue.add(index, request)
                    }
                }
            }
        }
    }

    private fun executeNextRequest() {
        synchronized(this) {
            connHandler.removeMessages(MSG_REQUEST_TIMEOUT)
            if (requestQueue.isEmpty()) {
                currentRequest = null
            } else {
                executeRequest(requestQueue.removeAt(0))
            }
        }
    }

    private class ConnHandler internal constructor(connection: BaseConnection) : Handler(Looper.getMainLooper()) {
        private val weakRef: WeakReference<BaseConnection> = WeakReference(connection)

        override fun handleMessage(msg: Message) {
            val connection = weakRef.get()
            if (connection != null) {
                when (msg.what) {
                    MSG_REQUEST_TIMEOUT -> {
                        val request = msg.obj as Request
                        if (connection.currentRequest != null && connection.currentRequest === request) {
                            connection.handleFailedCallback(request, IConnection.REQUEST_FAIL_TYPE_REQUEST_TIMEOUT, false)
                            connection.executeNextRequest()
                        }
                    }
                }
                connection.handleMsg(msg)
            }
        }
    }

    protected abstract fun handleMsg(msg: Message)

    private fun executeRequest(request: Request) {
        currentRequest = request
        connHandler.sendMessageDelayed(Message.obtain(connHandler, MSG_REQUEST_TIMEOUT, request), config.requestTimeoutMillis.toLong())
        if (bluetoothAdapter!!.isEnabled) {
            if (bluetoothGatt != null) {
                when (request.type) {
                    Request.RequestType.READ_RSSI -> executeReadRssi(request)
                    Request.RequestType.CHANGE_MTU -> executeChangeMtu(request)
                    Request.RequestType.READ_PHY -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        bluetoothGatt!!.readPhy()
                    }
                    Request.RequestType.SET_PREFERRED_PHY -> executeSetPreferredPhy(request)
                    else -> {
                        val gattService = bluetoothGatt!!.getService(request.service)
                        if (gattService != null) {
                            val characteristic = gattService.getCharacteristic(request.characteristic)
                            if (characteristic != null) {
                                when (request.type) {
                                    Request.RequestType.ENABLE_NOTIFICATION,
                                    Request.RequestType.DISABLE_NOTIFICATION,
                                    Request.RequestType.ENABLE_INDICATION,
                                    Request.RequestType.DISABLE_INDICATION -> executeIndicationOrNotification(characteristic, request)
                                    Request.RequestType.READ_CHARACTERISTIC -> executeReadCharacteristic(characteristic, request)
                                    Request.RequestType.READ_DESCRIPTOR -> executeReadDescriptor(characteristic, request)
                                    Request.RequestType.WRITE_CHARACTERISTIC -> executeWriteCharacteristic(characteristic, request)
                                    else -> {
                                    }
                                }
                            } else {
                                handleFailedCallback(request, IConnection.REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC, true)
                            }
                        } else {
                            handleFailedCallback(request, IConnection.REQUEST_FAIL_TYPE_NULL_SERVICE, true)
                        }
                    }
                }
            } else {
                handleFailedCallback(request, IConnection.REQUEST_FAIL_TYPE_GATT_IS_NULL, true)
            }
        } else {
            handleFailedCallback(request, IConnection.REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED, true)
        }
    }

    private fun executeChangeMtu(request: Request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!bluetoothGatt!!.requestMtu(BleUtils.bytesToLong(false, *request.value!!).toInt())) {
                handleFailedCallback(request, IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
            }
        }
    }

    private fun executeReadRssi(request: Request) {
        if (!bluetoothGatt!!.readRemoteRssi()) {
            handleFailedCallback(request, IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
        }
    }

    private fun executeSetPreferredPhy(request: Request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val txPhy = BleUtils.bytesToLong(false, *Arrays.copyOfRange(request.value, 0, 4)).toInt()
            val rxPhy = BleUtils.bytesToLong(false, *Arrays.copyOfRange(request.value, 4, 8)).toInt()
            val phyOptions = BleUtils.bytesToLong(false, *Arrays.copyOfRange(request.value, 8, 12)).toInt()
            bluetoothGatt!!.setPreferredPhy(txPhy, rxPhy, phyOptions)
        }
    }

    private fun executeReadCharacteristic(characteristic: BluetoothGattCharacteristic, request: Request) {
        if (!bluetoothGatt!!.readCharacteristic(characteristic)) {
            handleFailedCallback(request, IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
        }
    }

    private fun executeWriteCharacteristic(characteristic: BluetoothGattCharacteristic, request: Request) {
        try {
            request.waitWriteResult = config.isWaitWriteResult
            request.writeDelay = config.packageWriteDelayMillis
            val requestWriteDelayMillis = config.requestWriteDelayMillis
            val reqDelay = if (requestWriteDelayMillis > 0) requestWriteDelayMillis else request.writeDelay
            if (reqDelay > 0) {
                Thread.sleep(reqDelay.toLong())
                if (request != currentRequest) {
                    return
                }
            }
            if (request.value!!.size > config.packageSize) {
                val list = BleUtils.splitPackage(request.value!!, config.packageSize)
                if (!request.waitWriteResult) { //without waiting
                    val delay = request.writeDelay.toLong()
                    for (i in list.indices) {
                        val bytes = list[i]
                        if (i > 0 && delay > 0) {
                            Thread.sleep(delay)
                            if (request != currentRequest) {
                                return
                            }
                        }
                        if (writeFail(characteristic, bytes)) {
                            handleWriteFailed(request)
                            return
                        }
                    }
                } else { //发送第一包，剩下的加入队列
                    request.remainQueue = ConcurrentLinkedQueue()
                    request.remainQueue!!.addAll(list)
                    request.sendingBytes = request.remainQueue!!.remove()
                    if (writeFail(characteristic, request.sendingBytes!!)) {
                        handleWriteFailed(request)
                        return
                    }
                }
            } else {
                request.sendingBytes = request.value
                if (writeFail(characteristic, request.value!!)) {
                    handleWriteFailed(request)
                    return
                }
            }
            if (!request.waitWriteResult) {
                if (request.callback != null) {
                    Ble.instance.getMethodPoster().post(currentRequest!!.callback!!, CharacteristicWriteCallback.getMethodInfo(device, request.tag, 
                            characteristic.service.uuid, characteristic.uuid, request.value!!))
                } else {
                    onCharacteristicWrite(request.tag, characteristic.service.uuid, characteristic.uuid, request.value!!)
                }
                executeNextRequest()
            }
        } catch (e: Exception) {
            handleWriteFailed(request)
        }
    }

    private fun handleWriteFailed(request: Request) {
        connHandler.removeMessages(MSG_REQUEST_TIMEOUT)
        request.remainQueue = null
        handleFailedCallback(request, IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
    }

    private fun writeFail(characteristic: BluetoothGattCharacteristic, value: ByteArray): Boolean {
        characteristic.value = value
        val writeType = config.getWriteType(characteristic.service.uuid, characteristic.uuid)
        if (writeType != null && (writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT || writeType == BluetoothGattCharacteristic.WRITE_TYPE_SIGNED ||
                        writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)) {
            characteristic.writeType = writeType
        }
        return !bluetoothGatt!!.writeCharacteristic(characteristic)
    }

    private fun executeReadDescriptor(characteristic: BluetoothGattCharacteristic, request: Request) {
        val gattDescriptor = characteristic.getDescriptor(request.descriptor)
        if (gattDescriptor != null) {
            if (!bluetoothGatt!!.readDescriptor(gattDescriptor)) {
                handleFailedCallback(request, IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
            }
        } else {
            handleFailedCallback(request, IConnection.REQUEST_FAIL_TYPE_NULL_DESCRIPTOR, true)
        }
    }

    private fun executeIndicationOrNotification(characteristic: BluetoothGattCharacteristic, request: Request) {
        pendingCharacteristic = characteristic
        val gattDescriptor = pendingCharacteristic!!.getDescriptor(IConnection.clientCharacteristicConfig)
        if (gattDescriptor == null || !bluetoothGatt!!.readDescriptor(gattDescriptor)) {
            handleFailedCallback(request, IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
        }
    }

    private fun enableNotificationOrIndicationFail(enable: Boolean, notification: Boolean, characteristic: BluetoothGattCharacteristic): Boolean {
        if (!bluetoothAdapter!!.isEnabled || bluetoothGatt == null || !bluetoothGatt!!.setCharacteristicNotification(characteristic, enable)) {
            return true
        }
        val descriptor = characteristic.getDescriptor(IConnection.clientCharacteristicConfig)
                ?: return true
        val oriaValue = descriptor.value
        if (currentRequest != null) {
            if (currentRequest!!.type == Request.RequestType.DISABLE_NOTIFICATION || currentRequest!!.type == Request.RequestType.ENABLE_NOTIFICATION ||
                    currentRequest!!.type == Request.RequestType.DISABLE_INDICATION || currentRequest!!.type == Request.RequestType.ENABLE_INDICATION) {
                currentRequest!!.value = oriaValue
            }
        }
        if (enable) {
            descriptor.value = if (notification) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
            descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
        // There was a bug in Android up to 6.0 where the descriptor was written using parent
        // characteristic's write type, instead of always Write With Response, as the spec says.
        val writeType = characteristic.writeType
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val result = bluetoothGatt!!.writeDescriptor(descriptor)
        if (!enable) {
            //还原原始值
            descriptor.value = oriaValue
        }
        characteristic.writeType = writeType
        return !result
    }

    fun isNotificationOrIndicationEnabled(characteristic: BluetoothGattCharacteristic): Boolean {
        val descriptor = characteristic.getDescriptor(IConnection.clientCharacteristicConfig) ?: return false
        return Arrays.equals(descriptor.value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) or
                Arrays.equals(descriptor.value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
    }

    fun isNotificationOrIndicationEnabled(service: UUID, characteristic: UUID): Boolean {
        val c = getCharacteristic(service, characteristic)
        if (c != null) {
            return isNotificationOrIndicationEnabled(c)
        }
        return false
    }

    companion object {
        private const val MSG_REQUEST_TIMEOUT = 0
        internal const val MSG_CONNECT = 1
        internal const val MSG_DISCONNECT = 2
        internal const val MSG_REFRESH = 3
        internal const val MSG_AUTO_REFRESH = 4
        internal const val MSG_TIMER = 5
        internal const val MSG_RELEASE = 6
        internal const val MSG_DISCOVER_SERVICES = 7
        internal const val MSG_ON_CONNECTION_STATE_CHANGE = 8
        internal const val MSG_ON_SERVICES_DISCOVERED = 9

        /**
         * Clears the internal cache and forces a refresh of the services from the remote device.
         */
        fun refresh(bluetoothGatt: BluetoothGatt): Boolean {
            try {
                val localMethod = bluetoothGatt.javaClass.getMethod("refresh")
                return localMethod.invoke(bluetoothGatt) as Boolean
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return false
        }
    }
}