package com.snail.easyble.callback

import android.bluetooth.BluetoothGattCharacteristic

import com.snail.easyble.annotation.InvokeThread

/**
 * 
 * date: 2018/12/2 10:38
 * author: zengfansheng
 */
interface CharacteristicChangedCallback {
    @InvokeThread
    fun onCharacteristicChanged(characteristic: BluetoothGattCharacteristic)
}
