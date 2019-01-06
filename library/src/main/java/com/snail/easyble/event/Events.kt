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
    class CharacteristicRead internal constructor(device: Device, tag: String, val characteristic: GattCharacteristic) : BothDeviceAndTagEvent<Device>(device, tag)

    /**
     * onCharacteristicWrite
     */
    class CharacteristicWrite internal constructor(device: Device, tag: String, val characteristic: GattCharacteristic) : BothDeviceAndTagEvent<Device>(device, tag)

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
    class DescriptorRead internal constructor(device: Device, tag: String, val descriptor: GattDescriptor) : BothDeviceAndTagEvent<Device>(device, tag)

    /**
     * indication
     */
    class IndicationChanged internal constructor(device: Device, tag: String, val descriptor: GattDescriptor, val isEnabled: Boolean) : BothDeviceAndTagEvent<Device>(device, tag)

    /**
     * onMtuChanged，MTU change success
     */
    class MtuChanged internal constructor(device: Device, tag: String,
                                         /** the new value of MTU  */
                                         @IntRange(from = 23, to = 517)
                                         val mtu: Int) : BothDeviceAndTagEvent<Device>(device, tag)

    /**
     * notification
     */
    class NotificationChanged internal constructor(device: Device, tag: String, val descriptor: GattDescriptor, val isEnabled: Boolean) : BothDeviceAndTagEvent<Device>(device, tag)

    /**
     * onReadRemoteRssi
     */
    class RemoteRssiRead internal constructor(device: Device, tag: String, val rssi: Int) : BothDeviceAndTagEvent<Device>(device, tag)

    /**
     * onPhyRead
     */
    class PhyRead internal constructor(device: Device, tag: String, val txPhy: Int, val rxPhy: Int) : BothDeviceAndTagEvent<Device>(device, tag)

    /**
     * onPhyUpdate
     */
    class PhyUpdate internal constructor(device: Device, tag: String, val txPhy: Int, val rxPhy: Int) : BothDeviceAndTagEvent<Device>(device, tag)
    
    /**
     * Request failure events, such as read characteristic, write characteristic, enable notifications, etc.
     */
    class RequestFailed internal constructor(val device: Device, val tag: String, val requestType: Request.RequestType,
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

    fun newCharacteristicRead(device: Device, tag: String, characteristic: GattCharacteristic): CharacteristicRead {
        return CharacteristicRead(device, tag, characteristic)
    }

    fun newCharacteristicWrite(device: Device, tag: String, characteristic: GattCharacteristic): CharacteristicWrite {
        return CharacteristicWrite(device, tag, characteristic)
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

    fun newDescriptorRead(device: Device, tag: String, descriptor: GattDescriptor): DescriptorRead {
        return DescriptorRead(device, tag, descriptor)
    }

    fun newIndicationChanged(device: Device, tag: String, descriptor: GattDescriptor, isEnabled: Boolean): IndicationChanged {
        return IndicationChanged(device, tag, descriptor, isEnabled)
    }

    fun newMtuChanged(device: Device, tag: String, @IntRange(from = 23, to = 517) mtu: Int): MtuChanged {
        return MtuChanged(device, tag, mtu)
    }

    fun newNotificationChanged(device: Device, tag: String, descriptor: GattDescriptor, isEnabled: Boolean): NotificationChanged {
        return NotificationChanged(device, tag, descriptor, isEnabled)
    }

    fun newRemoteRssiRead(device: Device, tag: String, rssi: Int): RemoteRssiRead {
        return RemoteRssiRead(device, tag, rssi)
    }

    fun newRequestFailed(device: Device, tag: String, requestType: Request.RequestType, failType: Int, src: ByteArray?): RequestFailed {
        return RequestFailed(device, tag, requestType, failType, src)
    }

    fun newLogChanged(log: String, level: Int): LogChanged {
        return LogChanged(log, level)
    }

    fun newPhyRead(device: Device, tag: String, txPhy: Int, rxPhy: Int): PhyRead {
        return PhyRead(device, tag, txPhy, rxPhy)
    }

    fun newPhyUpdate(device: Device, tag: String, txPhy: Int, rxPhy: Int): PhyUpdate {
        return PhyUpdate(device, tag, txPhy, rxPhy)
    }
}
