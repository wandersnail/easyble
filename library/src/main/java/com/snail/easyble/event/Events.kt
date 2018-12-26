package com.snail.easyble.event

import android.bluetooth.BluetoothAdapter
import android.support.annotation.IntRange
import com.snail.easyble.core.*

/**
 * date: 2018/5/29 09:25
 * author: zengfansheng
 */
object Events {
    
    class BluetoothStateChanged internal constructor(
            /**
             * One of [BluetoothAdapter.STATE_OFF], [BluetoothAdapter.STATE_TURNING_ON], [BluetoothAdapter.STATE_ON], [BluetoothAdapter.STATE_TURNING_OFF]
             */
            var state: Int)

    /**
     * onCharacteristicChanged
     */
    class CharacteristicChanged internal constructor(device: Device, val characteristic: GattCharacteristic) : DeviceEvent<Device>(device)

    /**
     * onCharacteristicRead
     */
    class CharacteristicRead internal constructor(device: Device, requestId: String, val characteristic: GattCharacteristic) : BothDeviceAndRequestIdEvent<Device>(device, requestId)

    /**
     * onCharacteristicWrite
     */
    class CharacteristicWrite internal constructor(device: Device, requestId: String, val characteristic: GattCharacteristic) : BothDeviceAndRequestIdEvent<Device>(device, requestId)

    class ConnectFailed internal constructor(val device: Device?,
                                            /**
                                             * Fail type. One of [IConnection.CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION], [IConnection.CONNECT_FAIL_TYPE_UNSPECIFIED_ADDRESS]
                                             */
                                            val type: Int)

    class ConnectionStateChanged internal constructor(device: Device,
                                                      /** One of [IConnection.STATE_DISCONNECTED], [IConnection.STATE_CONNECTING],
                                                       * [IConnection.STATE_SCANNING], [IConnection.STATE_CONNECTED], [IConnection.STATE_SERVICE_DISCOVERING],
                                                       * [IConnection.STATE_SERVICE_DISCOVERED]
                                                       */
                                                     val state: Int) : DeviceEvent<Device>(device)

    class ConnectTimeout internal constructor(device: Device,
                                             /**
                                              * One of [IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE], [IConnection.TIMEOUT_TYPE_CANNOT_CONNECT],
                                              * [IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES]
                                              */
                                             val type: Int) : DeviceEvent<Device>(device)

    /**
     * onDescriptorRead
     */
    class DescriptorRead internal constructor(device: Device, requestId: String, val descriptor: GattDescriptor) : BothDeviceAndRequestIdEvent<Device>(device, requestId)

    /**
     * indication
     */
    class IndicationChanged internal constructor(device: Device, requestId: String, val descriptor: GattDescriptor, val isEnabled: Boolean) : BothDeviceAndRequestIdEvent<Device>(device, requestId)

    /**
     * onMtuChanged，MTU change success
     */
    class MtuChanged internal constructor(device: Device, requestId: String,
                                         /** the new value of MTU  */
                                         @IntRange(from = 23, to = 517)
                                         val mtu: Int) : BothDeviceAndRequestIdEvent<Device>(device, requestId)

    /**
     * notification
     */
    class NotificationChanged internal constructor(device: Device, requestId: String, val descriptor: GattDescriptor, val isEnabled: Boolean) : BothDeviceAndRequestIdEvent<Device>(device, requestId)

    /**
     * onReadRemoteRssi
     */
    class RemoteRssiRead internal constructor(device: Device, requestId: String, val rssi: Int) : BothDeviceAndRequestIdEvent<Device>(device, requestId)

    /**
     * onPhyRead
     */
    class PhyRead internal constructor(device: Device, requestId: String, val txPhy: Int, val rxPhy: Int) : BothDeviceAndRequestIdEvent<Device>(device, requestId)

    /**
     * onPhyUpdate
     */
    class PhyUpdate internal constructor(device: Device, requestId: String, val txPhy: Int, val rxPhy: Int) : BothDeviceAndRequestIdEvent<Device>(device, requestId)
    
    /**
     * Request failure events, such as read characteristic, write characteristic, enable notifications, etc.
     */
    class RequestFailed internal constructor(val device: Device, val requestId: String, val requestType: Request.RequestType,
                                            /**
                                             * One of [IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED],
                                             * [IConnection.REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC],
                                             * [IConnection.REQUEST_FAIL_TYPE_NULL_DESCRIPTOR],
                                             * [IConnection.REQUEST_FAIL_TYPE_NULL_SERVICE],
                                             * [IConnection.REQUEST_FAIL_TYPE_GATT_STATUS_FAILED],
                                             * [IConnection.REQUEST_FAIL_TYPE_GATT_IS_NULL],
                                             * [IConnection.REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW],
                                             * [IConnection.REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED] etc.
                                             */
                                            val failType: Int, val src: ByteArray?)

    /**
     * log event
     */
    class LogChanged internal constructor(val log: String, val level: Int)

    fun newBluetoothStateChanged(state: Int): BluetoothStateChanged {
        return BluetoothStateChanged(state)
    }

    fun newCharacteristicChanged(device: Device, characteristic: GattCharacteristic): CharacteristicChanged {
        return CharacteristicChanged(device, characteristic)
    }

    fun newCharacteristicRead(device: Device, requestId: String, characteristic: GattCharacteristic): CharacteristicRead {
        return CharacteristicRead(device, requestId, characteristic)
    }

    fun newCharacteristicWrite(device: Device, requestId: String, characteristic: GattCharacteristic): CharacteristicWrite {
        return CharacteristicWrite(device, requestId, characteristic)
    }

    fun newConnectFailed(device: Device?, code: Int): ConnectFailed {
        return ConnectFailed(device, code)
    }

    fun newConnectionStateChanged(device: Device, state: Int): ConnectionStateChanged {
        return ConnectionStateChanged(device, state)
    }

    fun newConnectTimeout(device: Device, type: Int): ConnectTimeout {
        return ConnectTimeout(device, type)
    }

    fun newDescriptorRead(device: Device, requestId: String, descriptor: GattDescriptor): DescriptorRead {
        return DescriptorRead(device, requestId, descriptor)
    }

    fun newIndicationChanged(device: Device, requestId: String, descriptor: GattDescriptor, isEnabled: Boolean): IndicationChanged {
        return IndicationChanged(device, requestId, descriptor, isEnabled)
    }

    fun newMtuChanged(device: Device, requestId: String, @IntRange(from = 23, to = 517) mtu: Int): MtuChanged {
        return MtuChanged(device, requestId, mtu)
    }

    fun newNotificationChanged(device: Device, requestId: String, descriptor: GattDescriptor, isEnabled: Boolean): NotificationChanged {
        return NotificationChanged(device, requestId, descriptor, isEnabled)
    }

    fun newRemoteRssiRead(device: Device, requestId: String, rssi: Int): RemoteRssiRead {
        return RemoteRssiRead(device, requestId, rssi)
    }

    fun newRequestFailed(device: Device, requestId: String, requestType: Request.RequestType, failType: Int, src: ByteArray?): RequestFailed {
        return RequestFailed(device, requestId, requestType, failType, src)
    }

    fun newLogChanged(log: String, level: Int): LogChanged {
        return LogChanged(log, level)
    }

    fun newPhyRead(device: Device, requestId: String, txPhy: Int, rxPhy: Int): PhyRead {
        return PhyRead(device, requestId, txPhy, rxPhy)
    }

    fun newPhyUpdate(device: Device, requestId: String, txPhy: Int, rxPhy: Int): PhyUpdate {
        return PhyUpdate(device, requestId, txPhy, rxPhy)
    }
}
