package com.snail.easyble.callback

import com.snail.easyble.core.Device
import java.util.*

/**
 *
 *
 * date: 2019/1/26 14:52
 * author: zengfansheng
 */
interface NotificationChangedCallback : RequestFailedCallback {
    fun onNotificationChanged(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, isEnabled: Boolean)
}