package com.snail.easyble.core

/**
 * 描述:
 * 时间: 2018/8/5 18:28
 * 作者: zengfansheng
 */
open class BleConfig {
    /** 扫描过滤器 */
    var scanConfig = ScanConfig()
        protected set
    /** 连接进行配对的控制 */
    var bondController: IBondController? = null
        protected set    
    /** Device实例生成器 */
    var deviceCreator: IDeviceCreator? = null
        protected set

    /**
     * 设置扫描过滤器
     *
     * @param config 扫描结果处理
     */
    fun setScanConfig(config: ScanConfig): BleConfig {
        scanConfig = config
        return this
    }

    /**
     * 连接进行配对的控制
     */
    fun setBondController(bondController: IBondController?): BleConfig {
        this.bondController = bondController
        return this
    }

    /**
     * 设置Device实例生成器，在搜索结果中返回的实例
     */
    fun setDeviceCreator(creator: IDeviceCreator?): BleConfig {
        deviceCreator = creator
        return this
    }
}
