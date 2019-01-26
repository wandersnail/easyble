package com.snail.easyble.callback

import android.bluetooth.BluetoothAdapter
import android.support.annotation.IntRange
import com.snail.easyble.core.Device
import com.snail.easyble.core.IConnection
import com.snail.easyble.core.Request
import java.util.*

/**
 *
 *
 * date: 2019/1/26 10:32
 * author: zengfansheng
 */
open class SimpleEventObserver : EventObserver {
    /**
     * @param state One of [BluetoothAdapter.STATE_OFF], [BluetoothAdapter.STATE_TURNING_ON], [BluetoothAdapter.STATE_ON], [BluetoothAdapter.STATE_TURNING_OFF]
     */
    override fun onBluetoothStateChanged(state: Int) {}

    override fun onCharacteristicChanged(device: Device, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {}

    override fun onCharacteristicRead(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {}

    override fun onCharacteristicWrite(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {}

    /**
     * @param type Fail type. One of [IConnection.CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION], [IConnection.CONNECT_FAIL_TYPE_UNSPECIFIED_ADDRESS]
     */
    override fun onConnectFailed(device: Device?, type: Int) {}

    /** 
     * @param state One of [IConnection.STATE_DISCONNECTED], [IConnection.STATE_CONNECTING],
     * [IConnection.STATE_SCANNING], [IConnection.STATE_CONNECTED], [IConnection.STATE_SERVICE_DISCOVERING],
     * [IConnection.STATE_SERVICE_DISCOVERED]
     */
    override fun onConnectionStateChanged(device: Device) {}

    /**
     * @param type One of [IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE], [IConnection.TIMEOUT_TYPE_CANNOT_CONNECT],
     * [IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES]
     */
    override fun onConnectTimeout(device: Device, type: Int) {}

    override fun onDescriptorRead(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, value: ByteArray) {}

    override fun onIndicationChanged(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, isEnabled: Boolean) {}

    /** 
     * @param mtu the new value of MTU  
     */
    override fun onMtuChanged(device: Device, tag: String, @IntRange(from = 23, to = 517) mtu: Int) {}

    override fun onNotificationChanged(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, isEnabled: Boolean) {}

    override fun onRemoteRssiRead(device: Device, tag: String, rssi: Int) {}

    override fun onPhyRead(device: Device, tag: String, txPhy: Int, rxPhy: Int) {}

    override fun onPhyUpdate(device: Device, tag: String, txPhy: Int, rxPhy: Int) {}

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
    override fun onRequestFailed(device: Device, tag: String, requestType: Request.RequestType, failType: Int, src: ByteArray?) {}

    override fun onLogChanged(log: String, level: Int) {}
}