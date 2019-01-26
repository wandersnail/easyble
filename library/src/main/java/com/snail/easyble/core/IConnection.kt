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

        /** Normal failed request  */
        const val REQUEST_FAIL_TYPE_REQUEST_FAILED = 0
        const val REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC = 1
        const val REQUEST_FAIL_TYPE_NULL_DESCRIPTOR = 2
        const val REQUEST_FAIL_TYPE_NULL_SERVICE = 3
        /** status is not [BluetoothGatt.GATT_SUCCESS] */
        const val REQUEST_FAIL_TYPE_GATT_STATUS_FAILED = 4
        const val REQUEST_FAIL_TYPE_GATT_IS_NULL = 5
        const val REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW = 6
        const val REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED = 7
        const val REQUEST_FAIL_TYPE_REQUEST_TIMEOUT = 8
        const val REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED = 9
        const val REQUEST_FAIL_TYPE_CONNECTION_RELEASED = 10
        const val REQUEST_FAIL_TYPE_VALUE_IS_NULL_OR_EMPTY = 11

        //----------ble connection state-------------  
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_SCANNING = 2
        /** Connected, not discover services yet */
        const val STATE_CONNECTED = 3
        /** Connected and discovering services */
        const val STATE_SERVICE_DISCOVERING = 4
        /** Connected and the services have been discovered */
        const val STATE_SERVICE_DISCOVERED = 5
        /** the connection has been released */
        const val STATE_RELEASED = 6
        //----------connect timeout---------
        const val TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE = 0
        /** BLE advertisement has been found, but cannot connect to the device */
        const val TIMEOUT_TYPE_CANNOT_CONNECT = 1
        /** Connect succeeded, but cannot discover any service. [BluetoothGattCallback.onServicesDiscovered] has not been called back. */
        const val TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES = 2

        //-------------connect fail type-------------------
        const val CONNECT_FAIL_TYPE_UNSPECIFIED_ADDRESS = 1
        const val CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION = 2
        const val CONNECT_FAIL_TYPE_NON_CONNECTABLE = 3
    }
}
