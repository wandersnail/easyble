package com.snail.easyble.core

import android.bluetooth.BluetoothDevice

/**
 * [Device] instance creator. It used in when a BLE advertisement has been found.
 * 
 * date: 2018/12/20 09:27
 * author: zengfansheng
 */
interface IDeviceCreator {
    /**
     * Create a [Device] instance based on broadcast information and filter at the same time.
     * 
     * @param advData Raw bytes of scan record     * 
     * @return If return null，this scan result will be thrown away, and will not appear in search result callback. 
     * Otherwise return a [Device] instance.
     */
    fun valueOf(device: BluetoothDevice, advData: ByteArray?): Device?
}