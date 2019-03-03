package com.snail.easyble.callback

import android.Manifest
import com.snail.easyble.core.Device

/**
 * 
 * date: 2018/5/9 00:08
 */
interface ScanListener {

    /**
     * 蓝牙搜索开始
     */
    fun onScanStart()

    /**
     * 蓝牙搜索停止
     */
    fun onScanStop()

    /**
     * 搜索到BLE设备
     */
    fun onScanResult(device: Device)

    /**
     * 搜索错误
     * 
     * @param errorCode [ERROR_LACK_LOCATION_PERMISSION], [ERROR_LOCATION_SERVICE_CLOSED]
     */
    fun onScanError(errorCode: Int, errorMsg: String)

    companion object {
        /** 缺少定位权限 [Manifest.permission.ACCESS_COARSE_LOCATION] 或者 [Manifest.permission.ACCESS_FINE_LOCATION] */
        const val ERROR_LACK_LOCATION_PERMISSION = 0
        /** 系统位置服务未开启 */
        const val ERROR_LOCATION_SERVICE_CLOSED = 1
    }
}
