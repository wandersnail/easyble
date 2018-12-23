package com.snail.easyble.core

import android.bluetooth.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.snail.easyble.annotation.InvokeThread
import com.snail.easyble.annotation.RunOn
import com.snail.easyble.callback.CharacteristicChangedCallback
import com.snail.easyble.callback.RequestCallback
import com.snail.easyble.event.Events
import com.snail.easyble.util.BleUtils
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 描述: 蓝牙连接基类
 * 时间: 2018/4/11 16:37
 * 作者: zengfansheng
 */
@Suppress("LeakingThis")
abstract class BaseConnection internal constructor(device: Device, protected var bluetoothDevice: BluetoothDevice, config: ConnectionConfig) : BluetoothGattCallback(), IConnection {
    var bluetoothGatt: BluetoothGatt? = null
        protected set
    private var requestQueue: MutableList<Request> = ArrayList()
    protected var currentRequest: Request? = null
    private var pendingCharacteristic: BluetoothGattCharacteristic? = null
    protected var bluetoothAdapter: BluetoothAdapter? = null
    protected var isReleased: Boolean = false
    internal var connHandler: Handler
    private val mainHandler: Handler
    /** 当前连接的配置 */
    var config: ConnectionConfig
        protected set
    private var characteristicChangedCallback: CharacteristicChangedCallback? = null
    /** 当前连接的设备 */
    var device: Device? = null
        private set
    private val executorService: ExecutorService

    init {
        this.device = device
        this.config = config
        connHandler = ConnHandler(this)
        mainHandler = Handler(Looper.getMainLooper())
        executorService = Executors.newCachedThreadPool()
    }

    /**
     * 获取蓝牙服务列表
     */
    val gattServices: List<BluetoothGattService>
        get() = if (bluetoothGatt != null) {
            bluetoothGatt!!.services
        } else ArrayList()

    fun clearRequestQueue() {
        synchronized(this) {
            requestQueue.clear()
            currentRequest = null
        }
    }

    /**
     * 设置设备上报数据回调
     */
    fun setCharacteristicChangedCallback(characteristicChangedCallback: CharacteristicChangedCallback) {
        this.characteristicChangedCallback = characteristicChangedCallback
    }

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

    internal fun clearRequestQueueAndNotify() {
        synchronized(this) {
            for (request in requestQueue) {
                handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED, false)
            }
            if (currentRequest != null) {
                handleFaildCallback(currentRequest!!, IConnection.REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED, false)
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
        // 读取到值
        if (currentRequest != null) {
            if (currentRequest!!.type == Request.RequestType.READ_CHARACTERISTIC) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (currentRequest!!.callback != null) {
                        handleRequestCallback(currentRequest!!.callback!!, Events.newCharacteristicRead(device!!, currentRequest!!.requestId,
                                GattCharacteristic(characteristic.service.uuid, characteristic.uuid, characteristic.value)))
                    } else {
                        onCharacteristicRead(currentRequest!!.requestId, characteristic)
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
                    val charac = GattCharacteristic(characteristic.service.uuid, characteristic.uuid, currentRequest!!.value!!)
                    if (currentRequest!!.callback != null) {
                        handleRequestCallback(currentRequest!!.callback!!, Events.newCharacteristicWrite(device!!, currentRequest!!.requestId, charac))
                    } else {
                        onCharacteristicWrite(currentRequest!!.requestId, charac)
                    }
                    executeNextRequest()
                } else {
                    connHandler.removeMessages(MSG_REQUEST_TIMEOUT)
                    connHandler.sendMessageDelayed(Message.obtain(connHandler, MSG_REQUEST_TIMEOUT, currentRequest), config.requestTimeoutMillis.toLong())
                    try {
                        java.lang.Thread.sleep(currentRequest!!.writeDelay.toLong())
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                    currentRequest!!.sendingBytes = currentRequest!!.remainQueue!!.remove()
                    if (writeFail(characteristic, currentRequest!!.sendingBytes!!)) {
                        handleWriteFailed(currentRequest!!)
                    }
                }
            } else {
                handleFaildCallback(currentRequest!!, IConnection.REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, true)
            }
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        // 收到设备notify值 （设备上报值）
        onCharacteristicChanged(characteristic)
        if (characteristicChangedCallback != null) {
            try {
                val method = characteristicChangedCallback!!.javaClass.getMethod("onCharacteristicChanged", BluetoothGattCharacteristic::class.java)
                execute(method, Runnable { characteristicChangedCallback!!.onCharacteristicChanged(characteristic) })
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
        if (currentRequest != null) {
            if (currentRequest!!.type == Request.RequestType.READ_RSSI) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (currentRequest!!.callback != null) {
                        handleRequestCallback(currentRequest!!.callback!!, Events.newRemoteRssiRead(device!!, currentRequest!!.requestId, rssi))
                    } else {
                        onReadRemoteRssi(currentRequest!!.requestId, rssi)
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
            if (currentRequest!!.type == Request.RequestType.TOGGLE_NOTIFICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleGattStatusFailed()
                }
                if (characteristic.service.uuid == pendingCharacteristic!!.service.uuid && characteristic.uuid == pendingCharacteristic!!.uuid) {
                    if (enableNotificationOrIndicationFail(Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, currentRequest!!.value), true, characteristic)) {
                        handleGattStatusFailed()
                    }
                }
            } else if (currentRequest!!.type == Request.RequestType.TOGGLE_INDICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleGattStatusFailed()
                }
                if (characteristic.service.uuid == pendingCharacteristic!!.service.uuid && characteristic.uuid == pendingCharacteristic!!.uuid) {
                    if (enableNotificationOrIndicationFail(Arrays.equals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE, currentRequest!!.value), false, characteristic)) {
                        handleGattStatusFailed()
                    }
                }
            } else if (currentRequest!!.type == Request.RequestType.READ_DESCRIPTOR) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (currentRequest!!.callback != null) {
                        handleRequestCallback(currentRequest!!.callback!!, Events.newDescriptorRead(device!!, currentRequest!!.requestId,
                                GattDescriptor(characteristic.service.uuid, characteristic.uuid, descriptor.uuid, descriptor.value)))
                    } else {
                        onDescriptorRead(currentRequest!!.requestId, descriptor)
                    }
                } else {
                    handleGattStatusFailed()
                }
                executeNextRequest()
            }
        }
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        if (currentRequest != null) {
            if (currentRequest!!.type == Request.RequestType.TOGGLE_NOTIFICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleGattStatusFailed()
                } else {
                    val isEnabled = Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, currentRequest!!.value)
                    if (currentRequest!!.callback != null) {
                        val ch = descriptor.characteristic
                        handleRequestCallback(currentRequest!!.callback!!, Events.newNotificationChanged(device!!, currentRequest!!.requestId,
                                GattDescriptor(ch.service.uuid, ch.uuid, descriptor.uuid, descriptor.value), isEnabled))
                    } else {
                        onNotificationChanged(currentRequest!!.requestId, descriptor, isEnabled)
                    }
                }
                executeNextRequest()
            } else if (currentRequest!!.type == Request.RequestType.TOGGLE_INDICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleGattStatusFailed()
                } else {
                    val isEnabled = Arrays.equals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE, currentRequest!!.value)
                    if (currentRequest!!.callback != null) {
                        val ch = descriptor.characteristic
                        handleRequestCallback(currentRequest!!.callback!!, Events.newIndicationChanged(device!!, currentRequest!!.requestId,
                                GattDescriptor(ch.service.uuid, ch.uuid, descriptor.uuid, descriptor.value), isEnabled))
                    } else {
                        onIndicationChanged(currentRequest!!.requestId, descriptor, isEnabled)
                    }
                }
                executeNextRequest()
            }
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
        if (currentRequest != null) {
            if (currentRequest!!.type == Request.RequestType.CHANGE_MTU) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (currentRequest!!.callback != null) {
                        handleRequestCallback(currentRequest!!.callback!!, Events.newMtuChanged(device!!, currentRequest!!.requestId, mtu))
                    } else {
                        onMtuChanged(currentRequest!!.requestId, mtu)
                    }
                } else {
                    handleGattStatusFailed()
                }
                executeNextRequest()
            }
        }
    }

    //需要保证currentRequest不为空
    private fun handleGattStatusFailed() {
        handleFaildCallback(currentRequest!!, IConnection.REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, false)
    }

    private fun handleFaildCallback(requestId: String, requestType: Request.RequestType, failType: Int, value: ByteArray?, executeNext: Boolean) {
        onRequestFialed(requestId, requestType, failType, value)
        if (executeNext) {
            executeNextRequest()
        }
    }

    private fun handleFaildCallback(request: Request, failType: Int, executeNext: Boolean) {
        if (request.callback != null) {
            handleRequestCallback(request.callback!!, Events.newRequestFailed(device!!, request.requestId, request.type, failType, request.value))
        } else {
            onRequestFialed(request.requestId, request.type, failType, request.value)
        }
        if (executeNext) {
            executeNextRequest()
        }
    }

    private fun handleRequestCallback(callback: RequestCallback<*>, param: Any) {
        var method: Method? = null
        try {
            method = if (param is Events.RequestFailed) {
                callback.javaClass.getMethod("onFail", param.javaClass)
            } else {
                callback.javaClass.getMethod("onSuccess", param.javaClass)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        execute(method, Runnable { method?.invoke(callback, param) })
    }

    //根据方法上是否有相应的注解，决定回调线程
    internal fun execute(method: Method?, runnable: Runnable) {
        if (method != null) {
            val invokeThread = method.getAnnotation(InvokeThread::class.java)
            if (invokeThread == null || invokeThread.value === RunOn.POSTING) {
                runnable.run()
            } else if (invokeThread.value === RunOn.BACKGROUND) {
                executorService.execute(runnable)
            } else {
                mainHandler.post(runnable)
            }
        }
    }

    /**
     * 改变最大传输单元
     * @param requestId 请求码
     * @param mtu 最大传输单元
     */
    @JvmOverloads
    fun changeMtu(requestId: String, mtu: Int, callback: RequestCallback<Events.MtuChanged>? = null) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> enqueue(Request.newChangeMtuRequest(requestId, mtu, callback))
            callback != null -> callback.onFail(Events.newRequestFailed(device!!, requestId, Request.RequestType.CHANGE_MTU, IConnection.REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW, BleUtils.numberToBytes(false, mtu.toLong(), 2)))
            else -> handleFaildCallback(requestId, Request.RequestType.CHANGE_MTU, IConnection.REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW, BleUtils.numberToBytes(false, mtu.toLong(), 2), false)
        }
    }

    /*
     * 请求读取characteristic的值
     * @param requestId 请求码
     */
    @JvmOverloads
    fun readCharacteristic(requestId: String, service: UUID, characteristic: UUID, callback: RequestCallback<Events.CharacteristicRead>? = null) {
        enqueue(Request.newReadCharacteristicRequest(requestId, service, characteristic, callback))
    }

    /**
     * 打开Notifications
     *
     * @param requestId 请求码
     * @param enable    开启还是关闭
     */
    @JvmOverloads
    fun toggleNotification(requestId: String, service: UUID, characteristic: UUID, enable: Boolean, callback: RequestCallback<Events.NotificationChanged>? = null) {
        enqueue(Request.newToggleNotificationRequest(requestId, service, characteristic, enable, callback))
    }

    /**
     * @param enable 开启还是关闭
     */
    @JvmOverloads
    fun toggleIndication(requestId: String, service: UUID, characteristic: UUID, enable: Boolean, callback: RequestCallback<Events.IndicationChanged>? = null) {
        enqueue(Request.newToggleIndicationRequest(requestId, service, characteristic, enable, callback))
    }

    @JvmOverloads
    fun readDescriptor(requestId: String, service: UUID, characteristic: UUID, descriptor: UUID, callback: RequestCallback<Events.DescriptorRead>? = null) {
        enqueue(Request.newReadDescriptorRequest(requestId, service, characteristic, descriptor, callback))
    }

    @JvmOverloads
    fun writeCharacteristic(requestId: String, service: UUID, characteristic: UUID, value: ByteArray?, callback: RequestCallback<Events.CharacteristicWrite>? = null) {
        if (value == null || value.isEmpty()) {
            if (callback != null) {
                callback.onFail(Events.newRequestFailed(device!!, requestId, Request.RequestType.WRITE_CHARACTERISTIC, IConnection.REQUEST_FAIL_TYPE_VALUE_IS_NULL_OR_EMPTY, value!!))
            } else {
                handleFaildCallback(requestId, Request.RequestType.WRITE_CHARACTERISTIC, IConnection.REQUEST_FAIL_TYPE_VALUE_IS_NULL_OR_EMPTY, value, false)
            }
            return
        }
        enqueue(Request.newWriteCharacteristicRequest(requestId, service, characteristic, value, callback))
    }

    @JvmOverloads
    fun readRssi(requestId: String, callback: RequestCallback<Events.RemoteRssiRead>? = null) {
        enqueue(Request.newReadRssiRequest(requestId, callback))
    }

    private fun enqueue(request: Request) {
        if (isReleased) {
            handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_CONNECTION_RELEASED, false)
        } else {
            synchronized(this) {
                if (currentRequest == null) {
                    executeRequest(request)
                } else {
                    requestQueue.add(request)
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
                            connection.handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_REQUEST_TIMEOUT, false)
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
                    else -> {
                        val gattService = bluetoothGatt!!.getService(request.service)
                        if (gattService != null) {
                            val characteristic = gattService.getCharacteristic(request.characteristic)
                            if (characteristic != null) {
                                when (request.type) {
                                    Request.RequestType.TOGGLE_NOTIFICATION, Request.RequestType.TOGGLE_INDICATION -> executeIndicationOrNotification(characteristic, request)
                                    Request.RequestType.READ_CHARACTERISTIC -> executeReadCharacteristic(characteristic, request)
                                    Request.RequestType.READ_DESCRIPTOR -> executeReadDescriptor(characteristic, request)
                                    Request.RequestType.WRITE_CHARACTERISTIC -> executeWriteCharacteristic(characteristic, request)
                                    else -> {}
                                }
                            } else {
                                handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC, true)
                            }
                        } else {
                            handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_NULL_SERVICE, true)
                        }
                    }
                }
            } else {
                handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_GATT_IS_NULL, true)
            }
        } else {
            handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED, true)
        }
    }

    private fun executeChangeMtu(request: Request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!bluetoothGatt!!.requestMtu(BleUtils.bytesToLong(false, *request.value!!).toInt())) {
                handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
            }
        } else {
            handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW, true)
        }
    }

    private fun executeReadRssi(request: Request) {
        if (!bluetoothGatt!!.readRemoteRssi()) {
            handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
        }
    }

    private fun executeReadCharacteristic(characteristic: BluetoothGattCharacteristic, request: Request) {
        if (!bluetoothGatt!!.readCharacteristic(characteristic)) {
            handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
        }
    }

    private fun executeWriteCharacteristic(characteristic: BluetoothGattCharacteristic, request: Request) {
        try {
            request.waitWriteResult = config.isWaitWriteResult
            request.writeDelay = config.packageWriteDelayMillis
            val packSize = config.packageSize
            val requestWriteDelayMillis = config.requestWriteDelayMillis
            java.lang.Thread.sleep((if (requestWriteDelayMillis > 0) requestWriteDelayMillis else request.writeDelay).toLong())
            if (request.value!!.size > packSize) {
                val list = BleUtils.splitPackage(request.value!!, packSize)
                if (!request.waitWriteResult) { //不等待则遍历发送
                    for (i in list.indices) {
                        val bytes = list[i]
                        if (i > 0) {
                            java.lang.Thread.sleep(request.writeDelay.toLong())
                        }
                        if (writeFail(characteristic, bytes)) { //写失败
                            handleWriteFailed(request)
                            return
                        }
                    }
                } else { //等待则只直接发送第一包，剩下的添加到队列等待回调
                    request.remainQueue = ConcurrentLinkedQueue()
                    request.remainQueue!!.addAll(list)
                    request.sendingBytes = request.remainQueue!!.remove()
                    if (writeFail(characteristic, request.sendingBytes!!)) { //写失败
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
                    handleRequestCallback(request.callback!!, Events.newCharacteristicWrite(device!!, request.requestId, GattCharacteristic(characteristic.service.uuid, characteristic.uuid, request.value!!)))
                } else {
                    onCharacteristicWrite(request.requestId, GattCharacteristic(characteristic.service.uuid, characteristic.uuid, request.value!!))
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
        handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
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
                handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
            }
        } else {
            handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_NULL_DESCRIPTOR, true)
        }
    }

    private fun executeIndicationOrNotification(characteristic: BluetoothGattCharacteristic, request: Request) {
        pendingCharacteristic = characteristic
        val gattDescriptor = pendingCharacteristic!!.getDescriptor(IConnection.clientCharacteristicConfig)
        if (gattDescriptor == null || !bluetoothGatt!!.readDescriptor(gattDescriptor)) {
            handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
        }
    }

    private fun enableNotificationOrIndicationFail(enable: Boolean, notification: Boolean, characteristic: BluetoothGattCharacteristic): Boolean {
        //setCharacteristicNotification是设置本机
        if (!bluetoothAdapter!!.isEnabled || bluetoothGatt == null || !bluetoothGatt!!.setCharacteristicNotification(characteristic, enable)) {
            return true
        }
        val descriptor = characteristic.getDescriptor(IConnection.clientCharacteristicConfig) ?: return true
        if (enable) {
            descriptor.value = if (notification) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
            descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
        //部分蓝牙在Android6.0及以下需要设置写入类型为有响应的，否则会enable回调是成功，但是仍然无法收到notification数据
        val writeType = characteristic.writeType
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val result = bluetoothGatt!!.writeDescriptor(descriptor) //把设置写入蓝牙设备
        characteristic.writeType = writeType
        return !result
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

        /*
         * Clears the internal cache and forces a refresh of the services from the
         * remote device.
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