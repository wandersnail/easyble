package com.snail.easyble.callback

/**
 *
 *
 * date: 2019/3/5 09:39
 * author: zengfansheng
 */
interface LogCallback {
    fun onLog(priority: Int, log: String)
}