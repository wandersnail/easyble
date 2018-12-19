package com.snail.easyble.core

import java.util.*

/**
 * 描述:
 * 时间: 2018/7/24 10:01
 * 作者: zengfansheng
 */
class GattDescriptor(var serviceUuid: UUID, var characteristicUuid: UUID, var descriptorUuid: UUID, var value: ByteArray)
