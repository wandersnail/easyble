package com.snail.easyble.core

import android.bluetooth.BluetoothDevice

/**
 * Used to control if the bond (pairing) with the remote device needs to be cleared
 * 
 * date: 2018/4/17 15:43
 * author: zengfansheng
 */
interface RemoveBondFilter {
    fun accept(device: BluetoothDevice): Boolean
}
