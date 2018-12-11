package com.snail.easyble.callback;

/**
 * 描述: 初始化回调
 * 时间: 2018/4/12 17:16
 * 作者: zengfansheng
 */
public interface InitCallback {    
    /** BluetoothManager初始化失败 */
    int ERROR_INIT_FAIL = 1;
    
    /** 不支持BLE */
    int ERROR_NOT_SUPPORT_BLE = 2;

    /**
     * 初始化成功
     */
    void onSuccess();

    /**
     * 初始化失败
     * @param errorCode {@link #ERROR_NOT_SUPPORT_BLE}, {@link #ERROR_INIT_FAIL}
     */
    void onFail(int errorCode);
}
