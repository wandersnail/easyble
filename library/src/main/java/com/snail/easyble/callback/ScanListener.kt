package com.snail.easyble.callback

import android.Manifest
import com.snail.easyble.core.Device

/**
 * 
 * date: 2018/5/9 00:08
 */
interface ScanListener {

    /**
     * Callback when scan has been started
     */
    fun onScanStart()

    /**
     * Callback when scan has been stoped
     */
    fun onScanStop()

    /**
     * Callback when a BLE advertisement has been found.
     * 
     * @param device BLE device
     */
    fun onScanResult(device: Device)

    /**
     * Callback when error occurs
     * 
     * @param errorCode One of [ERROR_LACK_LOCATION_PERMISSION], [ERROR_LOCATION_SERVICE_CLOSED]
     */
    fun onScanError(errorCode: Int, errorMsg: String)

    companion object {
        /** lack permission [Manifest.permission.ACCESS_COARSE_LOCATION] or [Manifest.permission.ACCESS_FINE_LOCATION] */
        const val ERROR_LACK_LOCATION_PERMISSION = 0
        /** System location service is not enabled */
        const val ERROR_LOCATION_SERVICE_CLOSED = 1
    }
}
