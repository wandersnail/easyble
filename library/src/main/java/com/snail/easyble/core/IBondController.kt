package com.snail.easyble.core

/**
 * Control if bond when connect to GATT Server hosted.
 * 
 * date: 2018/5/26 16:36
 * author: zengfansheng
 */
interface IBondController {
    fun bond(device: Device): Boolean
}
