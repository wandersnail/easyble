package com.snail.easyble.util

import android.util.Log

/**
 * 蓝牙库相关日志输出
 * 
 * date: 2018/6/20 12:33
 * author: zengfansheng
 */
class BleLogger internal constructor() {
    var logCallback: Callback? = null
    var logEnabled = false
    var filter: Filter? = null

    @JvmOverloads
    fun handleLog(priority: Int, msg: String, type: Int = TYPE_GENERAL, tag: String = TAG) {
        logCallback?.onLog(tag, type, priority, msg)
        if (logEnabled && (filter == null || filter!!.accept(tag, type, priority, msg))) {
            Log.println(priority, tag, msg)
        }
    }
    
    interface Filter {
        /**
         * @param tag 日志打印时的设置的tag
         * @param type 日志类型。[]
         */
        fun accept(tag: String, type: Int, priority: Int, msg: String): Boolean
    }

    interface Callback {
        fun onLog(tag: String, type: Int, priority: Int, log: String)
    }
    
    companion object {
        private const val TAG = "EasyBle"
        /** 一般的 */
        const val TYPE_GENERAL = 0
        /** 搜索状态 */
        const val TYPE_SCAN_STATE = 1
        /** 连接状态 */
        const val TYPE_CONNECTION_STATE = 2
        const val TYPE_CHARACTERISTIC_READ = 3
        const val TYPE_CHARACTERISTIC_CHANGED = 4
        const val TYPE_READ_REMOTE_RSSI = 5
        const val TYPE_MTU_CHANGED = 6
        const val TYPE_REQUEST_FIALED = 7
        const val TYPE_DESCRIPTOR_READ = 8
        const val TYPE_NOTIFICATION_CHANGED = 9
        const val TYPE_INDICATION_CHANGED = 10
        const val TYPE_CHARACTERISTIC_WRITE = 11
        const val TYPE_PHY_READ = 12
        const val TYPE_PHY_UPDATE = 13
    }
}
