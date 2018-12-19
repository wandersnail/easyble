package com.snail.easyble.event

/**
 * 使用方法: 在要监听的类中实现接口，并在方法上添加上@Subscribe注解
 * 时间: 2018/9/8 11:58
 * 作者: zengfansheng
 */
interface IRequestFailedEvent {
    /**
     * 请求失败事件，如读特征值、写特征值、开启notification等等
     */
    fun onRequestFailed(event: Events.RequestFailed)
}
