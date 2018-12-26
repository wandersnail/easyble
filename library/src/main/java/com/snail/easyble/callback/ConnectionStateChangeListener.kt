package com.snail.easyble.callback

import com.snail.easyble.core.Device
import com.snail.easyble.core.IConnection

/**
 * date: 2018/6/15 01:00
 * author: zengfansheng
 */
interface ConnectionStateChangeListener {

    /**
     * Callback indicating when GATT client has connected/disconnected to/from a remote
     * GATT server.
     * 
     * device.connectionState. One of [IConnection.STATE_DISCONNECTED], [IConnection.STATE_CONNECTING],
     * [IConnection.STATE_SCANNING], [IConnection.STATE_CONNECTED], [IConnection.STATE_SERVICE_DISCOVERING],
     * [IConnection.STATE_SERVICE_DISCOVERED]
     */
    fun onConnectionStateChanged(device: Device)

    /**
     * Callback invoked when cannot connect
     * 
     * @param type Failure type. One of [IConnection.CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION],
     * [IConnection.CONNECT_FAIL_TYPE_UNSPECIFIED_ADDRESS]
     */
    fun onConnectFailed(device: Device?, type: Int)

    /**
     * Callback invoked when connect timeout
     * 
     * @param type Timeout type. One of [IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE], [IConnection.TIMEOUT_TYPE_CANNOT_CONNECT],
     * [IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES]
     */
    fun onConnectTimeout(device: Device, type: Int)
}
