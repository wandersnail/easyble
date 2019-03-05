package com.snail.easyble.core

import android.bluetooth.BluetoothAdapter
import com.snail.easyble.callback.*

/**
 *
 *
 * date: 2019/1/26 10:32
 * author: zengfansheng
 */
interface EventObserver : CharacteristicReadCallback, CharacteristicWriteCallback, RemoteRssiReadCallback, DescriptorReadCallback, NotificationChangedCallback, 
        IndicationChangedCallback, MtuChangedCallback, PhyReadCallback, PhyUpdateCallback, ConnectionStateChangeListener, CharacteristicChangedCallback {
    /**
     * @param state 蓝牙开关状态。[BluetoothAdapter.STATE_OFF], [BluetoothAdapter.STATE_TURNING_ON], [BluetoothAdapter.STATE_ON], [BluetoothAdapter.STATE_TURNING_OFF]
     */
    fun onBluetoothStateChanged(state: Int)
}