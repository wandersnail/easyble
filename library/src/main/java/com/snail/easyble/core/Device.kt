package com.snail.easyble.core

import android.bluetooth.BluetoothDevice
import android.os.Parcel
import android.os.Parcelable

/**
 * 描述: 蓝牙设备
 * 时间: 2018/4/11 15:00
 * 作者: zengfansheng
 */
open class Device constructor(val originalDevice: BluetoothDevice) : Comparable<Device>, Cloneable, Parcelable {
    /** 广播数据 */
    var advData: ByteArray? = null

    /** 设备名称 */
    var name = ""

    /** 设备id */
    var devId = ""

    /** 设备地址 */
    var addr = originalDevice.address!!
        private set

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
        internal set

    init {
        name = originalDevice.name ?: ""
        bondState = originalDevice.bondState
    }

    val isConnected: Boolean
        get() = connectionState == IConnection.STATE_SERVICE_DISCOVERED

    val isDisconnected: Boolean
        get() = connectionState == IConnection.STATE_DISCONNECTED || connectionState == IConnection.STATE_RELEASED

    val isConnecting: Boolean
        get() = connectionState != IConnection.STATE_DISCONNECTED && connectionState != IConnection.STATE_SERVICE_DISCOVERED &&
                connectionState != IConnection.STATE_RELEASED

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

    constructor(source: Parcel) : this(source.readParcelable<BluetoothDevice>(BluetoothDevice::class.java.classLoader)!!) {
        advData = source.createByteArray()
        name = source.readString()!!
        devId = source.readString()!!
        addr = source.readString()!!
        firmware = source.readString()!!
        hardware = source.readString()!!
        type = source.readInt()
        battery = source.readInt()
        rssi = source.readInt()
        connectionState = source.readInt()
        bondState = source.readInt()
        isConnectable = when (source.readInt()) {
            1 -> true
            2 -> false
            else -> null
        }
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        dest.writeParcelable(originalDevice, flags)
        dest.writeByteArray(advData)
        dest.writeString(name)
        dest.writeString(devId)
        dest.writeString(addr)
        dest.writeString(firmware)
        dest.writeString(hardware)
        dest.writeInt(type)
        dest.writeInt(battery)
        dest.writeInt(rssi)
        dest.writeInt(connectionState)
        dest.writeInt(bondState)
        dest.writeInt(if (isConnectable == null) 0 else if (isConnectable!!) 1 else 2)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Device> = object : Parcelable.Creator<Device> {
            override fun createFromParcel(source: Parcel): Device = Device(source)
            override fun newArray(size: Int): Array<Device?> = arrayOfNulls(size)
        }
    }
}
