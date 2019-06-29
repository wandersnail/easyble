package com.snail.easyble.core

import com.snail.easyble.annotation.RunOn

/**
 *
 * date: 2018/8/5 18:28
 * author: zengfansheng
 */
open class BleConfig {
    /**
     * 搜索设置
     */
    open var scanConfig = ScanConfig()
    /**
     * 是否在连接时配对
     */
    open var bondController: IBondController? = null
    /**
     * [Device]实例构建器，搜索到BLE设备时，使用此构建器实例化[Device]
     */
    open var deviceCreator: IDeviceCreator? = null
    /**
     * 数据发布者（被观察者）
     */
    open var eventObservable: EventObservable = EventObservable()
    /**
     * 观察者或者回调的方法在没有使用注解指定调用线程时，默认被调用的线程
     */
    open var methodDefaultInvokeThread = RunOn.POSTING
}
