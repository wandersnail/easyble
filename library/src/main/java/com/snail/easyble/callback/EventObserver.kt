package com.snail.easyble.callback

import android.bluetooth.BluetoothAdapter

/**
 *
 *
 * date: 2019/1/26 10:32
 * author: zengfansheng
 */
interface EventObserver : CharacteristicReadCallback, CharacteristicWriteCallback, RemoteRssiReadCallback, DescriptorReadCallback,
        NotificationChangedCallback, IndicationChangedCallback, MtuChangedCallback, PhyReadCallback, PhyUpdateCallback, ConnectionStateChangeListener,
        CharacteristicChangedCallback {
    /**
     * @param state One of [BluetoothAdapter.STATE_OFF], [BluetoothAdapter.STATE_TURNING_ON], [BluetoothAdapter.STATE_ON], [BluetoothAdapter.STATE_TURNING_OFF]
     */
    fun onBluetoothStateChanged(state: Int)

    fun onLogChanged(log: String, level: Int)
}