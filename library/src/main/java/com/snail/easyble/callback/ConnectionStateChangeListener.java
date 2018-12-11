package com.snail.easyble.callback;

import android.support.annotation.NonNull;

import com.snail.easyble.core.Connection;
import com.snail.easyble.core.Device;

/**
 * 描述: 蓝牙连接状态回调
 * 时间: 2018/6/15 01:00
 * 作者: zengfansheng
 */
public interface ConnectionStateChangeListener {
    
    /**
     * 连接状态变化
     * @param device 设备。device.connectionState: 连接状态<br> {@link Connection#STATE_DISCONNECTED}<br> {@link Connection#STATE_CONNECTING}<br>
     *              {@link Connection#STATE_SCANNING}<br> {@link Connection#STATE_CONNECTED}<br>
     *              {@link Connection#STATE_SERVICE_DISCOVERING}<br> {@link Connection#STATE_SERVICE_DISCOVERED}
     */
    void onConnectionStateChanged(@NonNull Device device);

    /**
     * 连接失败
     * @param type 失败类型。<br>{@link Connection#CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION}<br> {@link Connection#CONNECT_FAIL_TYPE_UNSPECIFIED_MAC_ADDRESS}
     */
    void onConnectFailed(@NonNull Device device, int type);

    /**
     * 连接超时
     * @param type 超时类型。<br>{@link Connection#TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE}<br>{@link Connection#TIMEOUT_TYPE_CANNOT_CONNECT}<br>
     *          {@link Connection#TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES}
     */
    void onConnectTimeout(@NonNull Device device, int type);
}
