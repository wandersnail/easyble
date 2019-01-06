package com.snail.easyble.event

import com.snail.easyble.core.Device

/**
 * date: 2018/5/19 19:09
 * author: zengfansheng
 */
open class BothDeviceAndTagEvent<D : Device>(val device: D, val tag: String)
