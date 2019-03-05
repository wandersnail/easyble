package com.snail.easyble.core

import androidx.annotation.IntRange
import com.snail.easyble.callback.*
import java.util.*

/**
 * 消息发布者、被观察者
 *
 * date: 2019/1/26 10:34
 * author: zengfansheng
 */
open class EventObservable {
    /**
     * 注册的观察者
     */
    private val mObservers = ArrayList<EventObserver>()

    /**
     * 将观察者添加到注册集合里
     * 
     * @param observer 需要注册的观察者
     * @throws IllegalStateException 如果观察者已注册将抛此异常
     */
    fun registerObserver(observer: EventObserver) {
        synchronized(mObservers) {
            if (mObservers.contains(observer)) {
                throw IllegalStateException("Observer $observer is already registered.")
            }
            mObservers.add(observer)
        }
    }

    /**
     * 观察者是否已注册
     */
    fun isRegistered(observer: EventObserver): Boolean {
        synchronized(mObservers) {
            return mObservers.contains(observer)
        }
    } 
    
    /**
     * 将观察者从注册集合里移除
     * 
     * @param observer 需要取消注册的观察者
     */
    fun unregisterObserver(observer: EventObserver) {
        synchronized(mObservers) {
            val index = mObservers.indexOf(observer)
            if (index != -1) {
                mObservers.removeAt(index)
            }            
        }
    }

    /**
     * 将所有观察者从注册集合中移除
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
    
    protected fun notifyAll(methodName: String, valueTypePairs: Array<ValueTypePair>?) {
        getObservers().forEach {
            Ble.instance.getMethodPoster().post(it, methodName, valueTypePairs)
        }
    }
    
    protected fun notifyAll(methodInfo: MethodInfo) {
        notifyAll(methodInfo.name, methodInfo.valueTypePairs)
    }
    
    internal fun notifyBluetoothStateChanged(state: Int) {
        notifyAll("onBluetoothStateChanged", arrayOf(ValueTypePair(state, Int::class.java)))
    }

    internal fun notifyCharacteristicChanged(device: Device, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {
        notifyAll(CharacteristicChangedCallback.getMethodInfo(device, serviceUuid, characteristicUuid, value))
    }

    internal fun notifyCharacteristicRead(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {
        notifyAll(CharacteristicReadCallback.getMethodInfo(device, tag, serviceUuid, characteristicUuid, value))
    }

    internal fun notifyCharacteristicWrite(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {
        notifyAll(CharacteristicWriteCallback.getMethodInfo(device, tag, serviceUuid, characteristicUuid, value))
    }

    /**
     * @param type 连接失败类型。[IConnection.CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION], [IConnection.CONNECT_FAIL_TYPE_UNSPECIFIED_ADDRESS]
     */
    internal fun notifyConnectFailed(device: Device?, type: Int) {
        notifyAll("onConnectFailed", arrayOf(ValueTypePair(device, Device::class.java), ValueTypePair(type, Int::class.java)))
    }

    /**
     * device.connectionState 设备连接状态。[IConnection.STATE_DISCONNECTED], [IConnection.STATE_CONNECTING],
     * [IConnection.STATE_SCANNING], [IConnection.STATE_CONNECTED], [IConnection.STATE_SERVICE_DISCOVERING],
     * [IConnection.STATE_SERVICE_DISCOVERED]
     */
    internal fun notifyConnectionStateChanged(device: Device) {
        notifyAll("onConnectionStateChanged", arrayOf(ValueTypePair(device, Device::class.java)))
    }

    /**
     * @param type 连接超时类型。[IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE], [IConnection.TIMEOUT_TYPE_CANNOT_CONNECT],
     * [IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES]
     */
    internal fun notifyConnectTimeout(device: Device, type: Int) {
        notifyAll("onConnectTimeout", arrayOf(ValueTypePair(device, Device::class.java), ValueTypePair(type, Int::class.java)))
    }

    internal fun notifyDescriptorRead(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, value: ByteArray) {
        notifyAll(DescriptorReadCallback.getMethodInfo(device, tag, serviceUuid, characteristicUuid, descriptorUuid, value))
    }

    internal fun notifyIndicationChanged(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, isEnabled: Boolean) {
        notifyAll(IndicationChangedCallback.getMethodInfo(device, tag, serviceUuid, characteristicUuid, descriptorUuid, isEnabled))
    }

    /**
     * @param mtu 新的MTU
     */
    internal fun notifyMtuChanged(device: Device, tag: String, @IntRange(from = 23, to = 517) mtu: Int) {
        notifyAll(MtuChangedCallback.getMethodInfo(device, tag, mtu))
    }

    internal fun notifyNotificationChanged(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, isEnabled: Boolean) {
        notifyAll(NotificationChangedCallback.getMethodInfo(device, tag, serviceUuid, characteristicUuid, descriptorUuid, isEnabled))
    }

    internal fun notifyRemoteRssiRead(device: Device, tag: String, rssi: Int) {
        notifyAll(RemoteRssiReadCallback.getMethodInfo(device, tag, rssi))
    }

    internal fun notifyPhyRead(device: Device, tag: String, txPhy: Int, rxPhy: Int) {
        notifyAll(PhyReadCallback.getMethodInfo(device, tag, txPhy, rxPhy))
    }

    internal fun notifyPhyUpdate(device: Device, tag: String, txPhy: Int, rxPhy: Int) {
        notifyAll(PhyUpdateCallback.getMethodInfo(device, tag, txPhy, rxPhy))
    }

    /**
     * @param failType 请求失败类型。 [IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED],
     * [IConnection.REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC],
     * [IConnection.REQUEST_FAIL_TYPE_NULL_DESCRIPTOR],
     * [IConnection.REQUEST_FAIL_TYPE_NULL_SERVICE],
     * [IConnection.REQUEST_FAIL_TYPE_GATT_STATUS_FAILED],
     * [IConnection.REQUEST_FAIL_TYPE_GATT_IS_NULL],
     * [IConnection.REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW],
     * [IConnection.REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED] etc.
     */
    internal fun notifyRequestFailed(device: Device, tag: String, requestType: Request.RequestType, failType: Int, src: ByteArray?) {
        notifyAll(RequestFailedCallback.getMethodInfo(device, tag, requestType, failType, src))
    }
}