package com.snail.easyble.event

import android.bluetooth.BluetoothAdapter
import com.snail.easyble.core.*

/**
 * 描述: 事件统一管理
 * 时间: 2018/5/29 09:25
 * 作者: zengfansheng
 */
object Events {
    /**
     * 蓝牙状态变化
     */
    class BluetoothStateChanged internal constructor(
            /**
             * 当前状态。可能的值：
             * - [BluetoothAdapter.STATE_OFF]
             * - [BluetoothAdapter.STATE_TURNING_ON]
             * - [BluetoothAdapter.STATE_ON]
             * - [BluetoothAdapter.STATE_TURNING_OFF]
             */
            var state: Int)

    /**
     * onCharacteristicChanged，收到设备notify值 （设备上报值）
     */
    class CharacteristicChanged internal constructor(device: Device, var characteristic: GattCharacteristic) : DeviceEvent<Device>(device)

    /**
     * onCharacteristicRead，读取到特征字的值
     */
    class CharacteristicRead internal constructor(device: Device, requestId: String, var characteristic: GattCharacteristic) : BothDeviceAndRequestIdEvent<Device>(device, requestId)

    /**
     * onCharacteristicWrite，写入成功
     */
    class CharacteristicWrite internal constructor(device: Device, requestId: String, var characteristic: GattCharacteristic) : BothDeviceAndRequestIdEvent<Device>(device, requestId)

    /**
     * 连接失败
     */
    class ConnectFailed internal constructor(device: Device,
                                            /**
                                             * 错误类型
                                             * - [IConnection.CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION]
                                             * - [IConnection.CONNECT_FAIL_TYPE_UNSPECIFIED_MAC_ADDRESS]
                                             */
                                            var type: Int) : DeviceEvent<Device>(device)

    /**
     * 连接状态变化
     */
    class ConnectionStateChanged internal constructor(device: Device,
                                                     /**
                                                      * 当前连接状态。可能的值：
                                                      * - [IConnection.STATE_DISCONNECTED]
                                                      * - [IConnection.STATE_CONNECTING]
                                                      * - [IConnection.STATE_SCANNING]
                                                      * - [IConnection.STATE_CONNECTED]
                                                      * - [IConnection.STATE_SERVICE_DISCOVERING]
                                                      * - [IConnection.STATE_SERVICE_DISCOVERED]
                                                      * - [IConnection.STATE_RELEASED]
                                                      */
                                                     var state: Int) : DeviceEvent<Device>(device)

    /**
     * 连接超时
     */
    class ConnectTimeout internal constructor(device: Device,
                                             /**
                                              * 设备连接超时。可能的值：
                                              * - [IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE]
                                              * - [IConnection.TIMEOUT_TYPE_CANNOT_CONNECT]
                                              * - [IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES]
                                              */
                                             var type: Int) : DeviceEvent<Device>(device)

    /**
     * 读到indicator值
     */
    class DescriptorRead internal constructor(device: Device, requestId: String, var descriptor: GattDescriptor) : BothDeviceAndRequestIdEvent<Device>(device, requestId)

    /**
     * indication开关状态变化
     */
    class IndicationChanged internal constructor(device: Device, requestId: String, var descriptor: GattDescriptor, var isEnabled: Boolean) : BothDeviceAndRequestIdEvent<Device>(device, requestId)

    /**
     * onMtuChanged，MTU修改成功
     */
    class MtuChanged internal constructor(device: Device, requestId: String,
                                         /** 新的MTU值  */
                                         var mtu: Int) : BothDeviceAndRequestIdEvent<Device>(device, requestId)

    /**
     * notification开关状态变化
     */
    class NotificationChanged internal constructor(device: Device, requestId: String, var descriptor: GattDescriptor, var isEnabled: Boolean) : BothDeviceAndRequestIdEvent<Device>(device, requestId)

    /**
     * onReadRemoteRssi，读取到信息强度
     */
    class RemoteRssiRead internal constructor(device: Device, requestId: String, var rssi: Int) : BothDeviceAndRequestIdEvent<Device>(device, requestId)

    /**
     * 请求失败事件，如读特征值、写特征值、开启notification等等
     */
    class RequestFailed internal constructor(var device: Device, var requestId: String, var requestType: Request.RequestType,
                                            /**
                                             * - [IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED]
                                             * - [IConnection.REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC]
                                             * - [IConnection.REQUEST_FAIL_TYPE_NULL_DESCRIPTOR]
                                             * - [IConnection.REQUEST_FAIL_TYPE_NULL_SERVICE]
                                             * - [IConnection.REQUEST_FAIL_TYPE_GATT_STATUS_FAILED]
                                             * - [IConnection.REQUEST_FAIL_TYPE_GATT_IS_NULL]
                                             * - [IConnection.REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW]
                                             * - [IConnection.REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED]
                                             */
                                            var failType: Int,
                                            /** 请求时带的数据  */
                                            var src: ByteArray?)

    /**
     * 日志事件
     */
    class LogChanged internal constructor(var log: String, var level: Int)

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

    fun newConnectFailed(device: Device, code: Int): ConnectFailed {
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

    fun newMtuChanged(device: Device, requestId: String, mtu: Int): MtuChanged {
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
}
