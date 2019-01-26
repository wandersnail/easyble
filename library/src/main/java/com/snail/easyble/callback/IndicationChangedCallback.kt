package com.snail.easyble.callback

import com.snail.easyble.core.Device
import java.util.*

/**
 *
 *
 * date: 2019/1/26 14:56
 * author: zengfansheng
 */
interface IndicationChangedCallback : RequestFailedCallback {
    fun onIndicationChanged(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, isEnabled: Boolean)
}