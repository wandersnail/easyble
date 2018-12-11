package com.snail.easyble.core;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * 描述: 蓝牙设备
 * 时间: 2018/4/11 15:00
 * 作者: zengfansheng
 */
public class Device implements Comparable<Device>, Cloneable, Parcelable {
    /**
     * 原始设备
     */
    public BluetoothDevice originalDevice;
    /**
     * 广播数据
     */
    public byte[] scanRecord;
    /**
     * 设备名称
     */
    public String name = "";
    /**
     * 设备id
     */
    public String devId = "";
    /**
     * 设备地址
     */
    public String addr = "";
    /**
     * 固件版本
     */
    public String firmware = "";
    /**
     * 硬件版本
     */
    public String hardware = "";
    /**
     * 设备类型
     */
    public int type = -1;
    /**
     * 电量
     */
    public int battery = -1;
    /**
     * 信号强度
     */
    public int rssi = -1000;
    /**
     * 工作模式
     */
    public int mode;
    /**
     * 连接状态
     */
    public int connectionState = Connection.STATE_DISCONNECTED;
    /**
     * 配对状态
     */
    public int bondState;

    public Device() {
    }

    @Override
    public Device clone() {
        Device device = null;
        try {
            device = (Device) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return device;
    }

    public static Device valueOf(@NonNull BluetoothDevice bluetoothDevice) {
        Device device = new Device();
        device.name = bluetoothDevice.getName();
        device.addr = bluetoothDevice.getAddress();
        device.bondState = bluetoothDevice.getBondState();
        device.originalDevice = bluetoothDevice;
        return device;
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Device && addr.equals(((Device) obj).addr);
    }

    @Override
    public int hashCode() {
        return addr.hashCode();
    }

    @Override
    public int compareTo(@NonNull Device another) {
        int result;
        if (rssi == 0) {
            return -1;
        } else if (another.rssi == 0) {
            return 1;
        } else {
            result = Integer.compare(another.rssi, rssi);
            if (result == 0) {
                result = name.compareTo(another.name);
            }
        }
        return result;
    }

    public boolean isConnected() {
        return connectionState == Connection.STATE_SERVICE_DISCOVERED;
    }

    public boolean isDisconnected() {
        return connectionState == Connection.STATE_DISCONNECTED || connectionState == Connection.STATE_RELEASED;
    }

    public boolean isConnecting() {
        return connectionState != Connection.STATE_DISCONNECTED && connectionState != Connection.STATE_SERVICE_DISCOVERED &&
                connectionState != Connection.STATE_RELEASED;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.originalDevice, flags);
        dest.writeByteArray(this.scanRecord);
        dest.writeString(this.name);
        dest.writeString(this.devId);
        dest.writeString(this.addr);
        dest.writeString(this.firmware);
        dest.writeString(this.hardware);
        dest.writeInt(this.type);
        dest.writeInt(this.battery);
        dest.writeInt(this.rssi);
        dest.writeInt(this.mode);
        dest.writeInt(this.connectionState);
        dest.writeInt(this.bondState);
    }

    protected Device(Parcel in) {
        this.originalDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
        this.scanRecord = in.createByteArray();
        this.name = in.readString();
        this.devId = in.readString();
        this.addr = in.readString();
        this.firmware = in.readString();
        this.hardware = in.readString();
        this.type = in.readInt();
        this.battery = in.readInt();
        this.rssi = in.readInt();
        this.mode = in.readInt();
        this.connectionState = in.readInt();
        this.bondState = in.readInt();
    }

    public static final Parcelable.Creator<Device> CREATOR = new Parcelable.Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel source) {
            return new Device(source);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };
}
