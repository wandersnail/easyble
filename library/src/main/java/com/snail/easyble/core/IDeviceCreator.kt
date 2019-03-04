package com.snail.easyble.core

import android.bluetooth.BluetoothDevice

/**
 * [Device]实例构建器，搜索到BLE设备时，使用此构建器实例化[Device]
 * 
 * date: 2018/12/20 09:27
 * author: zengfansheng
 */
interface IDeviceCreator {
    /**
     * 搜索到蓝牙设备后，根据广播数据实例化[Device]，并且根据广播过滤设备
     * 
     * @param advData 广播数据
     * @return 如果不是需要的设备，返回null，过滤掉，那么它不会触发搜索结果回调，否则返回实例化的[Device]，触发搜索结果回调
     */
    fun valueOf(device: BluetoothDevice, advData: ByteArray?): Device?
}