package com.snail.easyble.event

import com.snail.easyble.core.Device

/**
 * date: 2018/5/19 18:57
 * author: zengfansheng
 */
open class DeviceEvent<D : Device>(val device: D)
