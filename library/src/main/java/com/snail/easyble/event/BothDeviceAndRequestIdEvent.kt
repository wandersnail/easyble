package com.snail.easyble.event

import com.snail.easyble.core.Device

/**
 * 描述: 带请求ID和设备的事件
 * 时间: 2018/5/19 19:09
 * 作者: zengfansheng
 */
open class BothDeviceAndRequestIdEvent<D : Device>(
        /** 设备  */
        var device: D, var requestId: String)
