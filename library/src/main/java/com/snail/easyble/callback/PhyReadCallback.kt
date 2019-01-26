package com.snail.easyble.callback

import com.snail.easyble.core.Device

/**
 *
 *
 * date: 2019/1/26 14:59
 * author: zengfansheng
 */
interface PhyReadCallback : RequestFailedCallback {
    fun onPhyRead(device: Device, tag: String, txPhy: Int, rxPhy: Int)
}