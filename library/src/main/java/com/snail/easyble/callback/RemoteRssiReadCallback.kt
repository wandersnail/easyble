package com.snail.easyble.callback

import com.snail.easyble.core.Device
import com.snail.easyble.core.MethodInfo
import com.snail.easyble.core.ValueTypePair

/**
 *
 *
 * date: 2019/1/26 14:47
 * author: zengfansheng
 */
interface RemoteRssiReadCallback : RequestFailedCallback {
    companion object {
        internal fun getMethodInfo(device: Device, tag: String, rssi: Int): MethodInfo {
            return MethodInfo("onRemoteRssiRead", arrayOf(ValueTypePair(device, Device::class.java), 
                    ValueTypePair(tag, String::class.java), ValueTypePair(rssi, Int::class.java)))
        }
    }
    
    fun onRemoteRssiRead(device: Device, tag: String, rssi: Int)
}