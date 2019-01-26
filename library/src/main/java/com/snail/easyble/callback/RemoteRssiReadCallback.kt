package com.snail.easyble.callback

import com.snail.easyble.core.Device

/**
 *
 *
 * date: 2019/1/26 14:47
 * author: zengfansheng
 */
interface RemoteRssiReadCallback : RequestFailedCallback {
    fun onRemoteRssiRead(device: Device, tag: String, rssi: Int)
}