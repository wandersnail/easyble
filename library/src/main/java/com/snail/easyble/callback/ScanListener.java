package com.snail.easyble.callback;

import android.support.annotation.NonNull;

import com.snail.easyble.core.Device;

/**
 * 描述: 蓝牙扫描回调
 * 时间: 2018/5/9 00:08
 */
public interface ScanListener {
    /** 缺少定位权限 */
    int ERROR_LACK_LOCATION_PERMISSION = 0;
    /** 系统位置服务未开启 */
    int ERROR_LOCATION_SERVICE_CLOSED = 1;
    
    /**
     * 扫描开始
     */
    void onScanStart();

    /**
     * 扫描结束
     */
    void onScanStop();

    /**
     * 扫描结果
     * @param device 设备
     */
    void onScanResult(@NonNull Device device);

    /**
     * 扫描错误
     * @param errorCode {@link #ERROR_LACK_LOCATION_PERMISSION}, {@link #ERROR_LOCATION_SERVICE_CLOSED}
     */
    void onScanError(int errorCode, @NonNull String errorMsg);
}
