package com.snail.easyble.core

import android.bluetooth.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.annotation.IntRange
import android.support.annotation.RequiresApi
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
 * date: 2018/4/11 16:37
 * author: zengfansheng
 */
@Suppress("LeakingThis")
abstract class BaseConnection internal constructor(val device: Device, protected var bluetoothDevice: BluetoothDevice, val config: ConnectionConfig) : BluetoothGattCallback(), IConnection {
    var bluetoothGatt: BluetoothGatt? = null
        protected set
    private var requestQueue: MutableList<Request> = ArrayList()
    private var currentRequest: Request? = null
    private var pendingCharacteristic: BluetoothGattCharacteristic? = null
    protected var bluetoothAdapter: BluetoothAdapter? = null
    protected var isReleased: Boolean = false
    internal var connHandler: Handler
    private val mainHandler: Handler
    private var characteristicChangedCallback: CharacteristicChangedCallback? = null
    private val executorService: ExecutorService

    init {
        connHandler = ConnHandler(this)
        mainHandler = Handler(Looper.getMainLooper())
        executorService = Executors.newCachedThreadPool()
    }

    /**
     * Returns a list of GATT services offered by the remote device.
     */
    val gattServices: List<BluetoothGattService>
        get() = if (bluetoothGatt != null) {
            bluetoothGatt!!.services
        } else ArrayList()

    /**
     * clear request queue without callbacks or EventBus's events
     */
    fun clearRequestQueue() {
        synchronized(this) {
            requestQueue.clear()
            currentRequest = null
        }
    }

    /**
     * set the callback handler that will receive remote characteristic notification
     */
    fun setCharacteristicChangedCallback(characteristicChangedCallback: CharacteristicChangedCallback) {
        this.characteristicChangedCallback = characteristicChangedCallback
    }

    /**
     * clear request queue by request type without callbacks or EventBus's events
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
     * clear request queue by request type and call callbacks or post EventBus's evnets
     */
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
        if (currentRequest != null) {
            if (currentRequest!!.type == Request.RequestType.READ_CHARACTERISTIC) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (currentRequest!!.callback != null) {
                        handleRequestCallback(currentRequest!!.callback!!, Events.newCharacteristicRead(device, currentRequest!!.requestId,
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
                        handleRequestCallback(currentRequest!!.callback!!, Events.newCharacteristicWrite(device, currentRequest!!.requestId, charac))
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

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        if (currentRequest != null) {
            if (currentRequest!!.type == Request.RequestType.READ_RSSI) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (currentRequest!!.callback != null) {
                        handleRequestCallback(currentRequest!!.callback!!, Events.newRemoteRssiRead(device, currentRequest!!.requestId, rssi))
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
                        if (currentRequest!!.callback != null) {
                            handleRequestCallback(currentRequest!!.callback!!, Events.newDescriptorRead(device, currentRequest!!.requestId,
                                    GattDescriptor(characteristic.service.uuid, characteristic.uuid, descriptor.uuid, descriptor.value)))
                        } else {
                            onDescriptorRead(currentRequest!!.requestId, descriptor)
                        }
                    } else {
                        handleGattStatusFailed()
                    }
                    executeNextRequest()
                }
                else -> {}
            }
        }
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor, status: Int) {
        if (currentRequest != null) {
            if (currentRequest!!.type == Request.RequestType.ENABLE_NOTIFICATION || currentRequest!!.type == Request.RequestType.DISABLE_NOTIFICATION ||
                    currentRequest!!.type == Request.RequestType.ENABLE_INDICATION || currentRequest!!.type == Request.RequestType.DISABLE_INDICATION) {
                val localDescriptor = getDescriptor(descriptor.characteristic.service.uuid, descriptor.characteristic.uuid, IConnection.clientCharacteristicConfig)
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleGattStatusFailed()
                    localDescriptor?.value = currentRequest!!.value
                } else {
                    val isEnabled = currentRequest!!.type == Request.RequestType.ENABLE_NOTIFICATION || currentRequest!!.type == Request.RequestType.ENABLE_INDICATION
                    if (currentRequest!!.callback != null) {
                        val ch = descriptor.characteristic
                        val event = if (currentRequest!!.type == Request.RequestType.ENABLE_NOTIFICATION || currentRequest!!.type == Request.RequestType.DISABLE_NOTIFICATION) {
                            Events.newNotificationChanged(device, currentRequest!!.requestId, GattDescriptor(ch.service.uuid, ch.uuid, descriptor.uuid, descriptor.value), isEnabled)
                        } else {
                            Events.newIndicationChanged(device, currentRequest!!.requestId, GattDescriptor(ch.service.uuid, ch.uuid, descriptor.uuid, descriptor.value), isEnabled)
                        }
                        handleRequestCallback(currentRequest!!.callback!!, event)
                    } else if (currentRequest!!.type == Request.RequestType.ENABLE_NOTIFICATION || currentRequest!!.type == Request.RequestType.DISABLE_NOTIFICATION) {
                        onNotificationChanged(currentRequest!!.requestId, descriptor, isEnabled)
                    } else {
                        onIndicationChanged(currentRequest!!.requestId, descriptor, isEnabled)
                    }
                }
                executeNextRequest()
            }
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        if (currentRequest != null) {
            if (currentRequest!!.type == Request.RequestType.CHANGE_MTU) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (currentRequest!!.callback != null) {
                        handleRequestCallback(currentRequest!!.callback!!, Events.newMtuChanged(device, currentRequest!!.requestId, mtu))
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

    override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
        handlePhyReadOrUpdate(true, gatt, txPhy, rxPhy, status)
    }

    override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
        handlePhyReadOrUpdate(false, gatt, txPhy, rxPhy, status)
    }

    private fun handlePhyReadOrUpdate(read: Boolean, gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
        if (currentRequest != null) {
            if ((read && currentRequest!!.type == Request.RequestType.READ_PHY) || ((!read && currentRequest!!.type == Request.RequestType.SET_PREFERRED_PHY))) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (currentRequest!!.callback != null) {
                        val event = if (read) {
                            Events.newPhyRead(device, currentRequest!!.requestId, txPhy, rxPhy)
                        } else {
                            Events.newPhyUpdate(device, currentRequest!!.requestId, txPhy, rxPhy)
                        }
                        handleRequestCallback(currentRequest!!.callback!!, event)
                    } else {
                        onPhyReadOrUpdate(currentRequest!!.requestId, read, txPhy, rxPhy)
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
            handleRequestCallback(request.callback, Events.newRequestFailed(device, request.requestId, request.type, failType, request.value))
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

    //Callback on different threads by annotation
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
     * change MTU(Maximum transmission unit)
     *
     * @param requestId Used to identify request tasks
     * @param mtu Maximum transmission unit
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @JvmOverloads
    fun changeMtu(requestId: String, @IntRange(from = 23, to = 517) mtu: Int, callback: RequestCallback<Events.MtuChanged>? = null, priority: Int = 0) {
        enqueue(Request.newChangeMtuRequest(requestId, mtu, callback, priority))
    }

    /**
     * Reads the requested characteristic from the associated remote device.
     */
    @JvmOverloads
    fun readCharacteristic(requestId: String, service: UUID, characteristic: UUID, callback: RequestCallback<Events.CharacteristicRead>? = null, priority: Int = 0) {
        if (checkUuidExists(requestId, Request.RequestType.READ_CHARACTERISTIC, null, callback, service, characteristic)) {
            enqueue(Request.newReadCharacteristicRequest(requestId, service, characteristic, callback, priority))
        }
    }

    @JvmOverloads
    fun enableNotification(requestId: String, service: UUID, characteristic: UUID, callback: RequestCallback<Events.NotificationChanged>? = null, priority: Int = 0) {
        if (checkUuidExists(requestId, Request.RequestType.ENABLE_NOTIFICATION, null, callback, service, characteristic)) {
            enqueue(Request.newEnableNotificationRequest(requestId, service, characteristic, callback, priority))
        }
    }

    @JvmOverloads
    fun disableNotification(requestId: String, service: UUID, characteristic: UUID, callback: RequestCallback<Events.NotificationChanged>? = null, priority: Int = 0) {
        if (checkUuidExists(requestId, Request.RequestType.DISABLE_NOTIFICATION, null, callback, service, characteristic)) {
            enqueue(Request.newDisableNotificationRequest(requestId, service, characteristic, callback, priority))
        }
    }

    @JvmOverloads
    fun enableIndication(requestId: String, service: UUID, characteristic: UUID, callback: RequestCallback<Events.IndicationChanged>? = null, priority: Int = 0) {
        if (checkUuidExists(requestId, Request.RequestType.ENABLE_INDICATION, null, callback, service, characteristic)) {
            enqueue(Request.newEnableIndicationRequest(requestId, service, characteristic, callback, priority))
        }
    }

    @JvmOverloads
    fun disableIndication(requestId: String, service: UUID, characteristic: UUID, callback: RequestCallback<Events.IndicationChanged>? = null, priority: Int = 0) {
        if (checkUuidExists(requestId, Request.RequestType.DISABLE_INDICATION, null, callback, service, characteristic)) {
            enqueue(Request.newDisableIndicationRequest(requestId, service, characteristic, callback, priority))
        }
    }

    /**
     * Reads the value for a given descriptor from the associated remote device.
     */
    @JvmOverloads
    fun readDescriptor(requestId: String, service: UUID, characteristic: UUID, descriptor: UUID, callback: RequestCallback<Events.DescriptorRead>? = null, priority: Int = 0) {
        if (checkUuidExists(requestId, Request.RequestType.READ_DESCRIPTOR, null, callback, service, characteristic)) {
            enqueue(Request.newReadDescriptorRequest(requestId, service, characteristic, descriptor, callback, priority))
        }
    }

    /**
     * Writes a given characteristic and its values to the associated remote device.
     */
    @JvmOverloads
    fun writeCharacteristic(requestId: String, service: UUID, characteristic: UUID, value: ByteArray?, callback: RequestCallback<Events.CharacteristicWrite>? = null, priority: Int = 0) {
        if (value == null || value.isEmpty()) {
            if (callback != null) {
                callback.onFail(Events.newRequestFailed(device, requestId, Request.RequestType.WRITE_CHARACTERISTIC, IConnection.REQUEST_FAIL_TYPE_VALUE_IS_NULL_OR_EMPTY, value!!))
            } else {
                handleFaildCallback(requestId, Request.RequestType.WRITE_CHARACTERISTIC, IConnection.REQUEST_FAIL_TYPE_VALUE_IS_NULL_OR_EMPTY, value, false)
            }
            return
        } else if (checkUuidExists(requestId, Request.RequestType.WRITE_CHARACTERISTIC, value, callback, service, characteristic)) {
            enqueue(Request.newWriteCharacteristicRequest(requestId, service, characteristic, value, callback, priority))
        }
    }

    /**
     * Read the RSSI for a connected remote device.
     */
    @JvmOverloads
    fun readRssi(requestId: String, callback: RequestCallback<Events.RemoteRssiRead>? = null, priority: Int = 0) {        
        enqueue(Request.newReadRssiRequest(requestId, callback, priority))
    }

    /**
     * Read the current transmitter PHY and receiver PHY of the connection. The values are returned
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @JvmOverloads
    fun readPhy(requestId: String, callback: RequestCallback<Events.RemoteRssiRead>? = null, priority: Int = 0) {
        enqueue(Request.newReadPhyRequest(requestId, callback, priority))
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
    fun setPreferredPhy(requestId: String, txPhy: Int, rxPhy: Int, phyOptions: Int, callback: RequestCallback<Events.RemoteRssiRead>? = null, priority: Int = 0) {
        enqueue(Request.newSetPreferredPhyRequest(requestId, txPhy, rxPhy, phyOptions, callback, priority))
    }

    //check whether the Service or Characteristic or Descriptore exists
    private fun checkUuidExists(requestId: String, requestType: Request.RequestType, src: ByteArray?, callback: RequestCallback<*>?, vararg uuids: UUID): Boolean {
        return when {
            uuids.isNotEmpty() -> {
                checkServiceExists(uuids[0], requestId, requestType, src, callback)
            }
            uuids.size > 1 -> {
                checkCharacteristicExists(uuids[0], uuids[1], requestId, requestType, src, callback)
            }
            uuids.size > 2 -> {
                checkDescriptoreExists(uuids[0], uuids[1], uuids[2], requestId, requestType, src, callback)
            }
            else -> false
        }
    }

    //check whether the Service exists
    private fun checkServiceExists(uuid: UUID, requestId: String, requestType: Request.RequestType, src: ByteArray?, callback: RequestCallback<*>?): Boolean {
        return if (getService(uuid) == null) {
            if (callback == null) {
                handleFaildCallback(requestId, requestType, IConnection.REQUEST_FAIL_TYPE_NULL_SERVICE, src, false)
            } else {
                handleRequestCallback(callback, Events.newRequestFailed(device, requestId, requestType, IConnection.REQUEST_FAIL_TYPE_NULL_SERVICE, src))
            }
            false
        } else true
    }

    //check whether the Characteristic exists
    private fun checkCharacteristicExists(service: UUID, characteristic: UUID, requestId: String, requestType: Request.RequestType,
                                          src: ByteArray?, callback: RequestCallback<*>?): Boolean {
        return if (checkServiceExists(service, requestId, requestType, src, callback)) {
            if (getCharacteristic(service, characteristic) == null) {
                if (callback == null) {
                    handleFaildCallback(requestId, requestType, IConnection.REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC, src, false)
                } else {
                    handleRequestCallback(callback, Events.newRequestFailed(device, requestId, requestType, IConnection.REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC, src))
                }
                false
            } else true
        } else false
    }

    //check whether the Descriptore exists
    private fun checkDescriptoreExists(service: UUID, characteristic: UUID, descriptor: UUID, requestId: String, requestType: Request.RequestType,
                                       src: ByteArray?, callback: RequestCallback<*>?): Boolean {
        return if (checkServiceExists(service, requestId, requestType, src, callback) && checkCharacteristicExists(service, characteristic,
                        requestId, requestType, src, callback)) {
            if (getDescriptor(service, characteristic, descriptor) == null) {
                if (callback == null) {
                    handleFaildCallback(requestId, requestType, IConnection.REQUEST_FAIL_TYPE_NULL_DESCRIPTOR, src, false)
                } else {
                    handleRequestCallback(callback, Events.newRequestFailed(device, requestId, requestType, IConnection.REQUEST_FAIL_TYPE_NULL_DESCRIPTOR, src))
                }
                false
            } else true
        } else false
    }

    private fun enqueue(request: Request) {
        if (isReleased) {
            handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_CONNECTION_RELEASED, false)
        } else {
            synchronized(this) {
                if (currentRequest == null) {
                    executeRequest(request)
                } else {                    
                    //Determine the location of the task in the queue based on priority
                    var index = -1
                    run {
                        requestQueue.forEachIndexed { i, req -> 
                            if (req.priority >= request.priority) {
                                if (i < requestQueue.size - 1) {
                                    if (requestQueue[i+1].priority < request.priority) {
                                        index = i + 1
                                        println("进来了")
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
        }
    }

    private fun executeReadRssi(request: Request) {
        if (!bluetoothGatt!!.readRemoteRssi()) {
            handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
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
            handleFaildCallback(request, IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
        }
    }

    private fun executeWriteCharacteristic(characteristic: BluetoothGattCharacteristic, request: Request) {
        try {
            request.waitWriteResult = config.isWaitWriteResult
            request.writeDelay = config.packageWriteDelayMillis
            val packSize = config.packageSize
            val requestWriteDelayMillis = config.requestWriteDelayMillis
            Thread.sleep((if (requestWriteDelayMillis > 0) requestWriteDelayMillis else request.writeDelay).toLong())
            if (request.value!!.size > packSize) {
                val list = BleUtils.splitPackage(request.value!!, packSize)
                if (!request.waitWriteResult) { //without waiting
                    for (i in list.indices) {
                        val bytes = list[i]
                        if (i > 0) {
                            Thread.sleep(request.writeDelay.toLong())
                        }
                        if (writeFail(characteristic, bytes)) {
                            handleWriteFailed(request)
                            return
                        }
                    }
                } else { //send the first package data and add the rest to the queue
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
                    handleRequestCallback(request.callback!!, Events.newCharacteristicWrite(device, request.requestId, GattCharacteristic(characteristic.service.uuid, characteristic.uuid, request.value!!)))
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
            //restore the original value
            descriptor.value = oriaValue
        }
        characteristic.writeType = writeType
        return !result
    }

    fun isNotificationOrIndicationEnabled(characteristic: BluetoothGattCharacteristic): Boolean {
        val descriptor = characteristic.getDescriptor(IConnection.clientCharacteristicConfig)
                ?: return false
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