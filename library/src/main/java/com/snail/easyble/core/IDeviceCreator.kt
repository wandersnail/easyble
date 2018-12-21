package com.snail.easyble.core

import android.bluetooth.BluetoothDevice

/**
 * 描述: 设备生成器，从广播结果创建设备
 * 时间: 2018/12/20 09:27
 * 作者: zengfansheng
 */
interface IDeviceCreator {
    /**
     * 根据广播信息创建Device，同时过滤
     * 
     * @param device 搜索到的设备
     * @param advData 广播内容
     * @return 返回null时，此搜索结果将被抛弃，不会出现在搜索结果回调中，否则返回创建的Device
     */
    fun valueOf(device: BluetoothDevice, advData: ByteArray?): Device?
}