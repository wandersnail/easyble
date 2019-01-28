package com.snail.easyble.callback

import com.snail.easyble.core.Device
import com.snail.easyble.core.MethodInfo
import com.snail.easyble.core.ValueTypePair
import java.util.*

/**
 * 
 * date: 2018/12/2 10:38
 * author: zengfansheng
 */
interface CharacteristicChangedCallback {
    companion object {
        internal fun getMethodInfo(device: Device, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray): MethodInfo {
            return MethodInfo("onCharacteristicChanged", arrayOf(ValueTypePair(device, Device::class.java), ValueTypePair(serviceUuid, UUID::class.java),
                    ValueTypePair(characteristicUuid, UUID::class.java), ValueTypePair(value, ByteArray::class.java)))
        }
    }
    
    fun onCharacteristicChanged(device: Device, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray)
}
