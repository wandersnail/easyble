package com.snail.easyble.callback

import com.snail.easyble.core.Device
import java.util.*

/**
 * 
 * date: 2018/12/2 10:38
 * author: zengfansheng
 */
interface CharacteristicChangedCallback {    
    fun onCharacteristicChanged(device: Device, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray)
}
