package com.snail.easyble.core

import android.bluetooth.le.ScanSettings
import java.util.*
import kotlin.collections.ArrayList

/**
 * 搜索配置
 * 
 * date: 2018/4/13 10:42
 * author: zengfansheng
 */
class ScanConfig {
    internal val uuids = ArrayList<UUID>()
    internal val names = ArrayList<String>()
    internal val addrs = ArrayList<String>()
    var scanPeriodMillis = 10000
        private set
    var isUseBluetoothLeScanner = true
        private set
    /** 是否将通过系统蓝牙配对连接的设备添加到搜索结果中（有些手机无法获取到系统已连接的蓝牙设备） */
    var isAcceptSysConnectedDevice = false
        private set
    /** 搜索设置 */
    var scanSettings: ScanSettings? = null
        private set
    /** 是否过滤非ble设备 */
    var isOnlyAcceptBleDevice = false
        private set
    var rssiLimit = Int.MIN_VALUE
        private set

    /**
     * 设置过滤设备名称
     */
    fun addAcceptNames(names: List<String>): ScanConfig {
        this.names.removeAll(names)
        this.names.addAll(names)
        return this
    }

    /**
     * 设置过滤设备地址
     */
    fun addAcceptAddrs(addrs: List<String>): ScanConfig {
        addrs.forEach { 
            val addr = it.toUpperCase(Locale.ENGLISH)
            if (!this.addrs.contains(addr)) {
                this.addrs.add(addr)
            }
        }
        return this
    }

    /**
     * 设置根据UUID过滤
     */
    fun addAcceptUuid(uuids: List<UUID>): ScanConfig {
        this.uuids.removeAll(uuids)
        this.uuids.addAll(uuids)
        return this
    }

    /**
     * 设置搜索周期
     */
    fun setScanPeriodMillis(scanPeriodMillis: Int): ScanConfig {
        this.scanPeriodMillis = scanPeriodMillis
        return this
    }

    /**设置是否使用新API的蓝牙搜索器
     * 
     */
    fun setUseBluetoothLeScanner(useBluetoothLeScanner: Boolean): ScanConfig {
        this.isUseBluetoothLeScanner = useBluetoothLeScanner
        return this
    }

    /**
     * 隐藏非BLE设备
     */
    fun setHideNonBleDevice(hideNonBleDevice: Boolean): ScanConfig {
        this.isOnlyAcceptBleDevice = hideNonBleDevice
        return this
    }

    /**
     * 是否将通过系统蓝牙配对连接的设备添加到搜索结果中（有些手机无法获取到系统已连接的蓝牙设备）
     */
    fun setAcceptSysConnectedDevice(acceptSysConnectedDevice: Boolean): ScanConfig {
        this.isAcceptSysConnectedDevice = acceptSysConnectedDevice
        return this
    }

    /**
     * 搜索设置
     */
    fun setScanSettings(scanSettings: ScanSettings): ScanConfig {
        this.scanSettings = scanSettings
        return this
    }

    /**
     * 设置信号强度过滤
     */
    fun setRssiLimit(rssiLimit: Int): ScanConfig {
        this.rssiLimit = rssiLimit
        return this
    }
}
