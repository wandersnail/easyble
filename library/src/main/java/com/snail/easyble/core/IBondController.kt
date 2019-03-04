package com.snail.easyble.core

/**
 * 控制连接时是否进行配对操作
 * 
 * date: 2018/5/26 16:36
 * author: zengfansheng
 */
interface IBondController {
    fun bond(device: Device): Boolean
}
