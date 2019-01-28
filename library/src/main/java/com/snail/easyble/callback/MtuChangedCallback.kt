package com.snail.easyble.callback

import android.support.annotation.IntRange
import com.snail.easyble.core.Device
import com.snail.easyble.core.MethodInfo
import com.snail.easyble.core.ValueTypePair

/**
 *
 *
 * date: 2019/1/26 14:57
 * author: zengfansheng
 */
interface MtuChangedCallback : RequestFailedCallback {
    companion object {
        internal fun getMethodInfo(device: Device, tag: String, @IntRange(from = 23, to = 517) mtu: Int): MethodInfo {
            return MethodInfo("onMtuChanged", arrayOf(ValueTypePair(device, Device::class.java), 
                    ValueTypePair(tag, String::class.java), ValueTypePair(mtu, Int::class.java)))
        }
    }
    
    /**
     * @param mtu the new value of MTU
     */
    fun onMtuChanged(device: Device, tag: String, @IntRange(from = 23, to = 517) mtu: Int)
}