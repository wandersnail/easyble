package com.snail.easyble.core

/**
 * 
 * date: 2018/8/5 18:28
 * author: zengfansheng
 */
open class BleConfig {
    /** Configuration of Bluetooth LE scan */
    var scanConfig = ScanConfig()
        protected set
    /** if bond when connect to GATT Server hosted */
    var bondController: IBondController? = null
        protected set    
    /** [Device] instance creator */
    var deviceCreator: IDeviceCreator? = null
        protected set

    /**
     * Configuration of Bluetooth LE scan
     */
    fun setScanConfig(config: ScanConfig): BleConfig {
        scanConfig = config
        return this
    }

    /**
     * Set if bond when connect to GATT Server hosted
     */
    fun setBondController(bondController: IBondController?): BleConfig {
        this.bondController = bondController
        return this
    }

    /**
     * Set [Device] instance creator. It used in when a BLE advertisement has been found.
     */
    fun setDeviceCreator(creator: IDeviceCreator?): BleConfig {
        deviceCreator = creator
        return this
    }
}
