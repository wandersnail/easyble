package com.snail.easyble.core

import android.bluetooth.le.ScanSettings
import java.util.*
import kotlin.collections.ArrayList

/**
 * Configuration of Bluetooth LE scan
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
    /** Whether add the connected [Device] that connected via system Bluetooth page to scan results */
    var isAcceptSysConnectedDevice = false
        private set
    /** See [ScanSettings] */
    var scanSettings: ScanSettings? = null
        private set
    /** If true, the non BLE devices will not appear in search result callback */
    var isOnlyAcceptBleDevice = false
        private set
    var rssiLimit = Int.MIN_VALUE
        private set

    fun addAcceptNames(names: List<String>): ScanConfig {
        this.names.removeAll(names)
        this.names.addAll(names)
        return this
    }

    fun addAcceptAddrs(addrs: List<String>): ScanConfig {
        addrs.forEach { 
            val addr = it.toUpperCase(Locale.ENGLISH)
            if (!this.addrs.contains(addr)) {
                this.addrs.add(addr)
            }
        }
        return this
    }

    fun addAcceptUuid(uuids: List<UUID>): ScanConfig {
        this.uuids.removeAll(uuids)
        this.uuids.addAll(uuids)
        return this
    }

    fun setScanPeriodMillis(scanPeriodMillis: Int): ScanConfig {
        this.scanPeriodMillis = scanPeriodMillis
        return this
    }

    fun setUseBluetoothLeScanner(useBluetoothLeScanner: Boolean): ScanConfig {
        this.isUseBluetoothLeScanner = useBluetoothLeScanner
        return this
    }

    /**
     * If true, the non BLE devices will not appear in search result callback
     */
    fun setHideNonBleDevice(hideNonBleDevice: Boolean): ScanConfig {
        this.isOnlyAcceptBleDevice = hideNonBleDevice
        return this
    }

    /**
     * Whether add the connected [Device] that connected via system Bluetooth page to scan results
     */
    fun setAcceptSysConnectedDevice(acceptSysConnectedDevice: Boolean): ScanConfig {
        this.isAcceptSysConnectedDevice = acceptSysConnectedDevice
        return this
    }

    /**
     * See [ScanSettings]
     */
    fun setScanSettings(scanSettings: ScanSettings): ScanConfig {
        this.scanSettings = scanSettings
        return this
    }
    
    fun setRssiLimit(rssiLimit: Int): ScanConfig {
        this.rssiLimit = rssiLimit
        return this
    }
}
