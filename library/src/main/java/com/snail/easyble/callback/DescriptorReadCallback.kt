package com.snail.easyble.callback

import com.snail.easyble.core.Device
import java.util.*

/**
 *
 *
 * date: 2019/1/26 14:49
 * author: zengfansheng
 */
interface DescriptorReadCallback : RequestFailedCallback {
    fun onDescriptorRead(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, value: ByteArray)
}