package com.snail.easyble.callback

import com.snail.easyble.core.Device
import com.snail.easyble.core.IConnection

/**
 * 描述: 蓝牙连接状态回调
 * 时间: 2018/6/15 01:00
 * 作者: zengfansheng
 */
interface ConnectionStateChangeListener {

    /**
     * 连接状态变化
     * @param device 设备。device.connectionState: 连接状态
     * - [IConnection.STATE_DISCONNECTED]
     * - [IConnection.STATE_CONNECTING]
     * - [IConnection.STATE_SCANNING]
     * - [IConnection.STATE_CONNECTED]<br></br>
     * - [IConnection.STATE_SERVICE_DISCOVERING]
     * - [IConnection.STATE_SERVICE_DISCOVERED]
     */
    fun onConnectionStateChanged(device: Device)

    /**
     * 连接失败
     * @param type 失败类型。
     * - [IConnection.CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION]
     * - [IConnection.CONNECT_FAIL_TYPE_UNSPECIFIED_MAC_ADDRESS]
     */
    fun onConnectFailed(device: Device, type: Int)

    /**
     * 连接超时
     * @param type 超时类型。
     * - [IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE]
     * - [IConnection.TIMEOUT_TYPE_CANNOT_CONNECT]<br></br>
     * - [IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES]
     */
    fun onConnectTimeout(device: Device, type: Int)
}
