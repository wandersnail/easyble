package com.snail.easyble.callback

import com.snail.easyble.core.Device
import java.util.*

/**
 *
 *
 * date: 2019/1/26 14:31
 * author: zengfansheng
 */
interface CharacteristicReadCallback : RequestFailedCallback {
    fun onCharacteristicRead(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray)
}