package com.snail.easyble.util

import android.util.Log
import com.snail.easyble.callback.LogCallback

/**
 * 蓝牙库相关日志输出
 * date: 2018/6/20 12:33
 * author: zengfansheng
 */
object BleLogger {
    private const val TAG = "EasyBle"                
    var logCallback: LogCallback? = null
    var logEnabled = false

    @JvmOverloads
    fun handleLog(priority: Int, msg: String, tag: String = TAG) {
        logCallback?.onLog(priority, msg)
        if (logEnabled) {
            Log.println(priority, tag, msg)
        }
    }
}
