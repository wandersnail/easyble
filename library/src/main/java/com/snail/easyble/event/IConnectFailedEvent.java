package com.snail.easyble.event;

import android.support.annotation.NonNull;

/**
 * 描述: 蓝牙连接事件
 * 使用方法: 在要监听的类中实现接口，并在方法上添加上@Subscribe注解
 * 时间: 2018/9/8 11:49
 * 作者: zengfansheng
 */
public interface IConnectFailedEvent {

    /**
     * 连接失败
     */
    void onConnectFailed(@NonNull Events.ConnectFailed event);
}
