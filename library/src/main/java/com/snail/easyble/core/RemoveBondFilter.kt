package com.snail.easyble.core

import android.bluetooth.BluetoothDevice

/**
 * 清空已配对设备时的过滤器
 * 
 * date: 2018/4/17 15:43
 * author: zengfansheng
 */
interface RemoveBondFilter {
    fun accept(device: BluetoothDevice): Boolean
}
