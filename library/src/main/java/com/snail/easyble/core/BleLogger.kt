package com.snail.easyble.core

import android.util.Log

/**
 * 蓝牙库相关日志输出
 * date: 2018/6/20 12:33
 * author: zengfansheng
 */
class BleLogger {

    /**
     * [NONE], [VERBOSE], [DEBUG], [INFO], [WARN], [ERROR]
     */
    private var printLevel = NONE
    private var filter: Filter? = null

    interface Filter {
        fun accept(log: String): Boolean
    }

    fun setPrintLevel(printLevel: Int) {
        this.printLevel = printLevel
    }

    fun setFilter(filter: Filter) {
        this.filter = filter
    }

    private fun accept(priority: Int): Boolean {
        val level = getLevel(priority)
        return printLevel and NONE != NONE && printLevel and level == level
    }

    internal fun println(tag: String, priority: Int, msg: String) {
        if (accept(priority) && (filter == null || filter!!.accept(msg))) {
            Log.println(priority, tag, msg)
        }
    }

    companion object {
        const val NONE = 1
        const val VERBOSE = NONE shl 1
        const val INFO = VERBOSE shl 1
        const val DEBUG = INFO shl 1
        const val WARN = DEBUG shl 1
        const val ERROR = WARN shl 1
        const val ALL = VERBOSE or INFO or DEBUG or WARN or ERROR

        fun getLevel(priority: Int): Int {
            return when (priority) {
                Log.ERROR -> ERROR
                Log.WARN -> WARN
                Log.INFO -> INFO
                Log.DEBUG -> DEBUG
                Log.VERBOSE -> VERBOSE
                else -> NONE
            }
        }
    }
}
