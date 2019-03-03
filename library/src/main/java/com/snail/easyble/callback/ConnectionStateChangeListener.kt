package com.snail.easyble.callback

import com.snail.easyble.core.Device
import com.snail.easyble.core.IConnection

/**
 * date: 2018/6/15 01:00
 * author: zengfansheng
 */
interface ConnectionStateChangeListener {

    /**
     * 连接状态变化回调
     * 
     * device.connectionState. [IConnection.STATE_DISCONNECTED], [IConnection.STATE_CONNECTING],
     * [IConnection.STATE_SCANNING], [IConnection.STATE_CONNECTED], [IConnection.STATE_SERVICE_DISCOVERING],
     * [IConnection.STATE_SERVICE_DISCOVERED]
     */
    fun onConnectionStateChanged(device: Device)

    /**
     * 无法连接时回调
     * 
     * @param type Failure type. [IConnection.CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION],
     * [IConnection.CONNECT_FAIL_TYPE_UNSPECIFIED_ADDRESS]
     */
    fun onConnectFailed(device: Device?, type: Int)

    /**
     * 连接超时
     * 
     * @param type Timeout type. [IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE], [IConnection.TIMEOUT_TYPE_CANNOT_CONNECT],
     * [IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES]
     */
    fun onConnectTimeout(device: Device, type: Int)
}
