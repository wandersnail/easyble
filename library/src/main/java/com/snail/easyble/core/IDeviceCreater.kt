package com.snail.easyble.core

import android.bluetooth.BluetoothDevice

/**
 * 描述: 设备生成器，从广播结果创建设备
 * 时间: 2018/12/20 09:27
 * 作者: zengfansheng
 */
interface IDeviceCreater {
    /**
     * 根据广播信息添创建Device
     * @param device 搜索到的设备
     * @param advData 广播内容
     * @return 如果不是想要的设备返回null，否则返回创建的Device
     */
    fun valueOf(device: BluetoothDevice, advData: ByteArray?): Device?
}