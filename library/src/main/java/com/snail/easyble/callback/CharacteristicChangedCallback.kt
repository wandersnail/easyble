package com.snail.easyble.callback

import android.bluetooth.BluetoothGattCharacteristic

import com.snail.easyble.annotation.InvokeThread

/**
 * 描述:
 * 时间: 2018/12/2 10:38
 * 作者: zengfansheng
 */
interface CharacteristicChangedCallback {
    /**
     * 收到设备notify值 （设备上报值）
     */
    @InvokeThread
    fun onCharacteristicChanged(characteristic: BluetoothGattCharacteristic)
}
