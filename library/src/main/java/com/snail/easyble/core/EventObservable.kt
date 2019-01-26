package com.snail.easyble.core

import android.support.annotation.IntRange
import com.snail.easyble.callback.EventObserver
import java.util.*

/**
 *
 *
 * date: 2019/1/26 10:34
 * author: zengfansheng
 */
open class EventObservable {
    /**
     * The list of observers.  An observer can be in the list at most
     * once and will never be null.
     */
    protected val mObservers = ArrayList<EventObserver>()

    /**
     * Adds an observer to the list. The observer cannot be null and it must not already
     * be registered.
     * @param observer the observer to register
     * @throws IllegalArgumentException the observer is null
     * @throws IllegalStateException the observer is already registered
     */
    fun registerObserver(observer: EventObserver) {
        synchronized(mObservers) {
            if (mObservers.contains(observer)) {
                throw IllegalStateException("Observer $observer is already registered.")
            }
            mObservers.add(observer)
        }
    }

    fun isRegistered(observer: EventObserver): Boolean {
        synchronized(mObservers) {
            return mObservers.contains(observer)
        }
    } 
    
    /**
     * Removes a previously registered observer. The observer must not be null and it
     * must already have been registered.
     * @param observer the observer to unregister
     * @throws IllegalArgumentException the observer is null
     * @throws IllegalStateException the observer is not yet registered
     */
    fun unregisterObserver(observer: EventObserver) {
        synchronized(mObservers) {
            val index = mObservers.indexOf(observer)
            if (index == -1) {
                throw IllegalStateException("Observer $observer was not registered.")
            }
            mObservers.removeAt(index)
        }
    }

    /**
     * Remove all registered observers.
     */
    fun unregisterAll() {
        synchronized(mObservers) {
            mObservers.clear()
        }
    }
    
    private fun getObservers(): Array<EventObserver> {
        synchronized(mObservers) {
            return mObservers.toTypedArray()
        }
    }
    
    protected fun notifyAll(methodName: String, params: Array<Any?>, paramTypes: Array<Class<*>>) {
        getObservers().forEach {
            val method = it.javaClass.getMethod(methodName, *paramTypes)
            Ble.instance.execute(method, Runnable {
                method.invoke(it, *params)
            })
        }
    }
    
    internal fun notifyBluetoothStateChanged(state: Int) {
        notifyAll("onBluetoothStateChanged", arrayOf(state), arrayOf(Int::class.java))
    }

    internal fun notifyCharacteristicChanged(device: Device, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {
        notifyAll("onCharacteristicChanged", arrayOf(device, serviceUuid, characteristicUuid, value),
            arrayOf(Device::class.java, UUID::class.java, UUID::class.java, ByteArray::class.java))
    }

    internal fun notifyCharacteristicRead(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {
        notifyAll("onCharacteristicRead", arrayOf(device, tag, serviceUuid, characteristicUuid, value),
            arrayOf(Device::class.java, String::class.java, UUID::class.java, UUID::class.java, ByteArray::class.java))
    }

    internal fun notifyCharacteristicWrite(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {
        notifyAll("onCharacteristicWrite", arrayOf(device, tag, serviceUuid, characteristicUuid, value),
            arrayOf(Device::class.java, String::class.java, UUID::class.java, UUID::class.java, ByteArray::class.java))
    }

    /**
     * @param type Fail type. One of [IConnection.CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION], [IConnection.CONNECT_FAIL_TYPE_UNSPECIFIED_ADDRESS]
     */
    internal fun notifyConnectFailed(device: Device?, type: Int) {
        notifyAll("onConnectFailed", arrayOf(device, type), arrayOf(Device::class.java, Int::class.java))
    }

    /**
     * device.connectionState. One of [IConnection.STATE_DISCONNECTED], [IConnection.STATE_CONNECTING],
     * [IConnection.STATE_SCANNING], [IConnection.STATE_CONNECTED], [IConnection.STATE_SERVICE_DISCOVERING],
     * [IConnection.STATE_SERVICE_DISCOVERED]
     */
    internal fun notifyConnectionStateChanged(device: Device) {
        notifyAll("onConnectionStateChanged", arrayOf(device), arrayOf(Device::class.java))
    }

    /**
     * @param type One of [IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE], [IConnection.TIMEOUT_TYPE_CANNOT_CONNECT],
     * [IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES]
     */
    internal fun notifyConnectTimeout(device: Device, type: Int) {
        notifyAll("onConnectTimeout", arrayOf(device, type), arrayOf(Device::class.java, Int::class.java))
    }

    internal fun notifyDescriptorRead(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, value: ByteArray) {
        notifyAll("onDescriptorRead", arrayOf(device, tag, serviceUuid, characteristicUuid, descriptorUuid, value),
            arrayOf(Device::class.java, String::class.java, UUID::class.java, UUID::class.java, UUID::class.java, ByteArray::class.java))
    }

    internal fun notifyIndicationChanged(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, isEnabled: Boolean) {
        notifyAll("onIndicationChanged", arrayOf(device, tag, serviceUuid, characteristicUuid, descriptorUuid, isEnabled),
            arrayOf(Device::class.java, String::class.java, UUID::class.java, UUID::class.java, UUID::class.java, Boolean::class.java))
    }

    /**
     * @param mtu the new value of MTU
     */
    internal fun notifyMtuChanged(device: Device, tag: String, @IntRange(from = 23, to = 517) mtu: Int) {
        notifyAll("onMtuChanged", arrayOf(device, tag, mtu), arrayOf(Device::class.java, String::class.java, Int::class.java))
    }

    internal fun notifyNotificationChanged(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, isEnabled: Boolean) {
        notifyAll("onNotificationChanged", arrayOf(device, tag, serviceUuid, characteristicUuid, descriptorUuid, isEnabled),
            arrayOf(Device::class.java, String::class.java, UUID::class.java, UUID::class.java, UUID::class.java, Boolean::class.java))
    }

    internal fun notifyRemoteRssiRead(device: Device, tag: String, rssi: Int) {
        notifyAll("onRemoteRssiRead", arrayOf(device, tag, rssi), arrayOf(Device::class.java, String::class.java, Int::class.java))
    }

    internal fun notifyPhyRead(device: Device, tag: String, txPhy: Int, rxPhy: Int) {
        notifyAll("onPhyRead", arrayOf(device, tag, txPhy, rxPhy), arrayOf(Device::class.java, String::class.java, Int::class.java, Int::class.java))
    }

    internal fun notifyPhyUpdate(device: Device, tag: String, txPhy: Int, rxPhy: Int) {
        notifyAll("onPhyUpdate", arrayOf(device, tag, txPhy, rxPhy), arrayOf(Device::class.java, String::class.java, Int::class.java, Int::class.java))
    }

    /**
     * @param failType One of [IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED],
     * [IConnection.REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC],
     * [IConnection.REQUEST_FAIL_TYPE_NULL_DESCRIPTOR],
     * [IConnection.REQUEST_FAIL_TYPE_NULL_SERVICE],
     * [IConnection.REQUEST_FAIL_TYPE_GATT_STATUS_FAILED],
     * [IConnection.REQUEST_FAIL_TYPE_GATT_IS_NULL],
     * [IConnection.REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW],
     * [IConnection.REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED] etc.
     */
    internal fun notifyRequestFailed(device: Device, tag: String, requestType: Request.RequestType, failType: Int, src: ByteArray?) {
        notifyAll("onRequestFailed", arrayOf(device, tag, requestType, failType, src),
            arrayOf(Device::class.java, String::class.java, Request.RequestType::class.java, Int::class.java, ByteArray::class.java))
    }

    internal fun notifyLogChanged(log: String, level: Int) {
        notifyAll("onLogChanged", arrayOf(log, level), arrayOf(String::class.java, Int::class.java))
    }
}