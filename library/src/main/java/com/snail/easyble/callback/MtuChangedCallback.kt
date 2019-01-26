package com.snail.easyble.callback

import android.support.annotation.IntRange
import com.snail.easyble.core.Device

/**
 *
 *
 * date: 2019/1/26 14:57
 * author: zengfansheng
 */
interface MtuChangedCallback : RequestFailedCallback {
    /**
     * @param mtu the new value of MTU
     */
    fun onMtuChanged(device: Device, tag: String, @IntRange(from = 23, to = 517) mtu: Int)
}