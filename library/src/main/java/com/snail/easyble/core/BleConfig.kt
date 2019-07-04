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
    /**
     * 自动重连时，搜索次数与间隔的对应关系，key：已尝试次数，value：间隔，单位为毫秒。如搜索了1次，间隔2秒，搜索了5次，间隔30秒等
     */
    open var scanIntervalPairsInAutoReonnection = mutableListOf(Pair(0, 2000), Pair(1, 5000), Pair(3, 10000), Pair(5, 30000), Pair(10, 60000))
}
