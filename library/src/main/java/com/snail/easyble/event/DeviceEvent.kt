package com.snail.easyble.event

import com.snail.easyble.core.Device

/**
 * 描述: 带设备的事件
 * 时间: 2018/5/19 18:57
 * 作者: zengfansheng
 */
open class DeviceEvent<D : Device>(
        /** 设备  */
        var device: D)
