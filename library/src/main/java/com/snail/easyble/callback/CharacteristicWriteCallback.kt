package com.snail.easyble.callback

import com.snail.easyble.core.Device
import java.util.*

/**
 *
 *
 * date: 2019/1/26 14:42
 * author: zengfansheng
 */
interface CharacteristicWriteCallback : RequestFailedCallback {
    fun onCharacteristicWrite(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray)
}