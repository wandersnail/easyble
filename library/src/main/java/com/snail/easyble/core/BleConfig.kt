package com.snail.easyble.core

import android.bluetooth.le.ScanSettings

/**
 * 描述:
 * 时间: 2018/8/5 18:28
 * 作者: zengfansheng
 */
class BleConfig {
    /** 扫描过滤器 */
    var scanHandler: ScanHandler? = null
        private set
    /** 连接进行配对的控制 */
    var bondController: IBondController? = null
        private set
    /** 蓝牙扫描周期 */
    var scanPeriodMillis = 10000
        private set
    /** 是否使用新版的扫描器 */
    var isUseBluetoothLeScanner = true
        private set
    /** 是否显示系统已连接设备 */
    var isAcceptSysConnectedDevice: Boolean = false
        private set
    /** 搜索设置，Android5.0以上有效 */
    var scanSettings: ScanSettings? = null
        private set
    /** 是否过滤非BLE设备 */
    var isHideNonBleDevice: Boolean = false
        private set

    /**
     * 设置扫描过滤器
     *
     * @param filter 扫描结果处理
     */
    fun setScanHandler(filter: ScanHandler): BleConfig {
        scanHandler = filter
        return this
    }

    /**
     * 设置蓝牙扫描周期
     *
     * @param scanPeriodMillis 毫秒
     */
    fun setScanPeriodMillis(scanPeriodMillis: Int): BleConfig {
        this.scanPeriodMillis = scanPeriodMillis
        return this
    }

    /**
     * 控制是否使用新版的扫描器
     */
    fun setUseBluetoothLeScanner(useBluetoothLeScanner: Boolean): BleConfig {
        this.isUseBluetoothLeScanner = useBluetoothLeScanner
        return this
    }

    /**
     * 连接进行配对的控制
     */
    fun setBondController(bondController: IBondController): BleConfig {
        this.bondController = bondController
        return this
    }

    /**
     * 设置是否显示系统已连接设备
     */
    fun setAcceptSysConnectedDevice(acceptSysConnectedDevice: Boolean): BleConfig {
        this.isAcceptSysConnectedDevice = acceptSysConnectedDevice
        return this
    }

    /**
     * 扫描设置
     */
    fun setScanSettings(scanSettings: ScanSettings): BleConfig {
        this.scanSettings = scanSettings
        return this
    }

    /**
     * 设置是否过滤非BLE设备
     */
    fun setHideNonBleDevice(hideNonBleDevice: Boolean): BleConfig {
        this.isHideNonBleDevice = hideNonBleDevice
        return this
    }
}
