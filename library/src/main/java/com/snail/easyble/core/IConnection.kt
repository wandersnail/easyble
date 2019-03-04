package com.snail.easyble.core

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import java.util.*

/**
 * 
 * date: 2018/6/23 18:58
 * author: zengfansheng
 */
interface IConnection {

    fun onCharacteristicRead(tag: String, characteristic: BluetoothGattCharacteristic)

    fun onCharacteristicChanged(characteristic: BluetoothGattCharacteristic)

    fun onReadRemoteRssi(tag: String, rssi: Int)

    fun onMtuChanged(tag: String, mtu: Int)

    fun onRequestFialed(tag: String, requestType: Request.RequestType, failType: Int, value: ByteArray?)

    fun onDescriptorRead(tag: String, descriptor: BluetoothGattDescriptor)

    fun onNotificationChanged(tag: String, descriptor: BluetoothGattDescriptor, isEnabled: Boolean)

    fun onIndicationChanged(tag: String, descriptor: BluetoothGattDescriptor, isEnabled: Boolean)

    fun onCharacteristicWrite(tag: String, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray)
        
    fun onPhyReadOrUpdate(tag: String, read: Boolean, txPhy: Int, rxPhy: Int)

    companion object {
        val clientCharacteristicConfig = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")!!

        /** 普通请求失败  */
        const val REQUEST_FAIL_TYPE_REQUEST_FAILED = 0
        const val REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC = 1
        const val REQUEST_FAIL_TYPE_NULL_DESCRIPTOR = 2
        const val REQUEST_FAIL_TYPE_NULL_SERVICE = 3
        /** 请求结果不是[BluetoothGatt.GATT_SUCCESS] */
        const val REQUEST_FAIL_TYPE_GATT_STATUS_FAILED = 4
        const val REQUEST_FAIL_TYPE_GATT_IS_NULL = 5
        const val REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW = 6
        const val REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED = 7
        const val REQUEST_FAIL_TYPE_REQUEST_TIMEOUT = 8
        const val REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED = 9
        const val REQUEST_FAIL_TYPE_CONNECTION_RELEASED = 10
        const val REQUEST_FAIL_TYPE_VALUE_IS_NULL_OR_EMPTY = 11

        //----------连接状态-------------  
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_SCANNING = 2
        /** 已连接，还未执行发现服务 */
        const val STATE_CONNECTED = 3
        /** 已连接，正在发现服务 */
        const val STATE_SERVICE_DISCOVERING = 4
        /** 已连接，成功发现服务 */
        const val STATE_SERVICE_DISCOVERED = 5
        /** 连接已释放 */
        const val STATE_RELEASED = 6
        //----------连接超时类型---------
        const val TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE = 0
        /** 搜索到设备，但是无法连接成功 */
        const val TIMEOUT_TYPE_CANNOT_CONNECT = 1
        /** 连接成功，但是无法发现蓝牙服务，即[BluetoothGattCallback.onServicesDiscovered]不回调 */
        const val TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES = 2

        //-------------连接失败类型-------------------
        const val CONNECT_FAIL_TYPE_UNSPECIFIED_ADDRESS = 1
        const val CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION = 2
        const val CONNECT_FAIL_TYPE_NON_CONNECTABLE = 3
    }
}
