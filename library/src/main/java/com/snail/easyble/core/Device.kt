package com.snail.easyble.core

import android.bluetooth.BluetoothDevice
import android.os.Parcel
import android.os.Parcelable

/**
 * 描述: 蓝牙设备
 * 时间: 2018/4/11 15:00
 * 作者: zengfansheng
 */
class Device : Comparable<Device>, Cloneable, Parcelable {
    /** 原始设备 */
    var originalDevice: BluetoothDevice? = null
    /** 广播数据 */
    var scanRecord: ByteArray? = null
    /** 设备名称 */
    var name = ""
    /** 设备id */
    var devId = ""
    /** 设备地址 */
    var addr = ""
    /** 固件版本 */
    var firmware = ""
    /** 硬件版本 */
    var hardware = ""
    /** 设备类型 */
    var type = -1
    /** 电量 */
    var battery = -1
    /** 信号强度 */
    var rssi = -1000
    /** 连接状态 */
    var connectionState = IConnection.STATE_DISCONNECTED
    /** 配对状态 */
    var bondState = BluetoothDevice.BOND_NONE
    /** 是否可连接，只在API 26及以上可获取 */
    var isConnectable: Boolean? = null

    val isConnected: Boolean
        get() = connectionState == IConnection.STATE_SERVICE_DISCOVERED

    val isDisconnected: Boolean
        get() = connectionState == IConnection.STATE_DISCONNECTED || connectionState == IConnection.STATE_RELEASED

    val isConnecting: Boolean
        get() = connectionState != IConnection.STATE_DISCONNECTED && connectionState != IConnection.STATE_SERVICE_DISCOVERED &&
                connectionState != IConnection.STATE_RELEASED

    constructor()

    override fun equals(other: Any?): Boolean {
        return other is Device && addr == other.addr
    }

    override fun hashCode(): Int {
        return addr.hashCode()
    }

    override fun compareTo(other: Device): Int {
        var result: Int
        if (rssi == 0) {
            return -1
        } else if (other.rssi == 0) {
            return 1
        } else {
            result = Integer.compare(other.rssi, rssi)
            if (result == 0) {
                result = name.compareTo(other.name)
            }
        }
        return result
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(this.originalDevice, flags)
        dest.writeByteArray(this.scanRecord)
        dest.writeString(this.name)
        dest.writeString(this.devId)
        dest.writeString(this.addr)
        dest.writeString(this.firmware)
        dest.writeString(this.hardware)
        dest.writeInt(this.type)
        dest.writeInt(this.battery)
        dest.writeInt(this.rssi)
        dest.writeInt(this.connectionState)
        dest.writeInt(this.bondState)
    }

    protected constructor(source: Parcel) {
        this.originalDevice = source.readParcelable(BluetoothDevice::class.java.classLoader)
        this.scanRecord = source.createByteArray()
        this.name = source.readString()!!
        this.devId = source.readString()!!
        this.addr = source.readString()!!
        this.firmware = source.readString()!!
        this.hardware = source.readString()!!
        this.type = source.readInt()
        this.battery = source.readInt()
        this.rssi = source.readInt()
        this.connectionState = source.readInt()
        this.bondState = source.readInt()
    }

    companion object {

        fun valueOf(bluetoothDevice: BluetoothDevice): Device {
            val device = Device()
            device.name = bluetoothDevice.name ?: ""
            device.addr = bluetoothDevice.address ?: ""
            device.bondState = bluetoothDevice.bondState
            device.originalDevice = bluetoothDevice
            return device
        }

        @JvmField
        val CREATOR: Parcelable.Creator<Device> = object : Parcelable.Creator<Device> {
            override fun createFromParcel(source: Parcel): Device {
                return Device(source)
            }

            override fun newArray(size: Int): Array<Device?> {
                return arrayOfNulls(size)
            }
        }
    }
}
