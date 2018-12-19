package com.snail.easyble.callback

import com.snail.easyble.core.Device

/**
 * 描述: 蓝牙扫描回调
 * 时间: 2018/5/9 00:08
 */
interface ScanListener {

    /**
     * 扫描开始
     */
    fun onScanStart()

    /**
     * 扫描结束
     */
    fun onScanStop()

    /**
     * 扫描结果
     * @param device 设备
     */
    fun onScanResult(device: Device)

    /**
     * 扫描错误
     * @param errorCode [.ERROR_LACK_LOCATION_PERMISSION], [.ERROR_LOCATION_SERVICE_CLOSED]
     */
    fun onScanError(errorCode: Int, errorMsg: String)

    companion object {
        /** 缺少定位权限  */
        const val ERROR_LACK_LOCATION_PERMISSION = 0
        /** 系统位置服务未开启  */
        const val ERROR_LOCATION_SERVICE_CLOSED = 1
    }
}
