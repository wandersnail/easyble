package com.snail.easyble.core

import android.bluetooth.le.ScanSettings
import java.util.*
import kotlin.collections.ArrayList

/**
 * 描述: 蓝牙搜索过滤器
 * 时间: 2018/4/13 10:42
 * 作者: zengfansheng
 */
class ScanConfig {
    internal val uuids = ArrayList<UUID>()
    internal val names = ArrayList<String>()
    internal val addrs = ArrayList<String>()
    /** 蓝牙扫描周期 */
    var scanPeriodMillis = 10000
        private set
    /** 是否使用新版的扫描器 */
    var isUseBluetoothLeScanner = true
        private set
    /** 是否显示系统已连接设备 */
    var isAcceptSysConnectedDevice = false
        private set
    /** 搜索设置，Android5.0以上有效 */
    var scanSettings: ScanSettings? = null
        private set
    /** 是否过滤非BLE设备 */
    var isHideNonBleDevice = false
        private set

    /**
     * 添加过滤的名称
     */
    fun addAcceptNames(names: List<String>): ScanConfig {
        this.names.removeAll(names)
        this.names.addAll(names)
        return this
    }

    /**
     * 添加过滤的蓝牙地址
     */
    fun addAcceptAddrs(addrs: List<String>): ScanConfig {
        addrs.forEach { 
            val addr = it.toUpperCase()
            if (!this.addrs.contains(addr)) {
                this.addrs.add(addr)
            }
        }
        return this
    }

    /**
     * 添加过滤的服务uuid
     */
    fun addAcceptServiceUuid(uuids: List<UUID>): ScanConfig {
        this.uuids.removeAll(uuids)
        this.uuids.addAll(uuids)
        return this
    }

    /**
     * 设置蓝牙扫描周期
     *
     * @param scanPeriodMillis 毫秒
     */
    fun setScanPeriodMillis(scanPeriodMillis: Int): ScanConfig {
        this.scanPeriodMillis = scanPeriodMillis
        return this
    }

    /**
     * 控制是否使用新版的扫描器
     */
    fun setUseBluetoothLeScanner(useBluetoothLeScanner: Boolean): ScanConfig {
        this.isUseBluetoothLeScanner = useBluetoothLeScanner
        return this
    }

    /**
     * 设置是否过滤非BLE设备
     */
    fun setHideNonBleDevice(hideNonBleDevice: Boolean): ScanConfig {
        this.isHideNonBleDevice = hideNonBleDevice
        return this
    }

    /**
     * 设置是否显示系统已连接设备
     */
    fun setAcceptSysConnectedDevice(acceptSysConnectedDevice: Boolean): ScanConfig {
        this.isAcceptSysConnectedDevice = acceptSysConnectedDevice
        return this
    }

    /**
     * 扫描设置
     */
    fun setScanSettings(scanSettings: ScanSettings): ScanConfig {
        this.scanSettings = scanSettings
        return this
    }
}
