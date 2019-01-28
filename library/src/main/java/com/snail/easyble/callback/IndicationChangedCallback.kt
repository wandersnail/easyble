package com.snail.easyble.callback

import com.snail.easyble.core.Device
import com.snail.easyble.core.MethodInfo
import com.snail.easyble.core.ValueTypePair
import java.util.*

/**
 *
 *
 * date: 2019/1/26 14:56
 * author: zengfansheng
 */
interface IndicationChangedCallback : RequestFailedCallback {
    companion object {
        internal fun getMethodInfo(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, isEnabled: Boolean): MethodInfo {
            return MethodInfo("onIndicationChanged", arrayOf(ValueTypePair(device, Device::class.java), ValueTypePair(tag, String::class.java), ValueTypePair(serviceUuid, UUID::class.java),
                    ValueTypePair(characteristicUuid, UUID::class.java), ValueTypePair(descriptorUuid, UUID::class.java), ValueTypePair(isEnabled, Boolean::class.java)))
        }
    }
    
    fun onIndicationChanged(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, isEnabled: Boolean)
}