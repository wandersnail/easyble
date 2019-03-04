package com.snail.easyble.core

/**
 * 
 * date: 2018/8/5 18:28
 * author: zengfansheng
 */
open class BleConfig {
    /** 搜索设置 */
    var scanConfig = ScanConfig()
        protected set
    /** 是否在连接时配对 */
    var bondController: IBondController? = null
        protected set    
    /** [Device]实例构建器，搜索到BLE设备时，使用此构建器实例化[Device] */
    var deviceCreator: IDeviceCreator? = null
        protected set
    var eventObservable: EventObservable = EventObservable()
        protected set

    /**
     * 数据发布者（被观察者）
     */
    fun setEventObservable(observable: EventObservable): BleConfig {
        eventObservable = observable
        return this
    }
    
    /**
     * 搜索设置
     */
    fun setScanConfig(config: ScanConfig): BleConfig {
        scanConfig = config
        return this
    }

    /**
     * 是否在连接时配对
     */
    fun setBondController(bondController: IBondController?): BleConfig {
        this.bondController = bondController
        return this
    }

    /**
     * [Device]实例构建器，搜索到BLE设备时，使用此构建器实例化[Device]
     */
    fun setDeviceCreator(creator: IDeviceCreator?): BleConfig {
        deviceCreator = creator
        return this
    }
}
