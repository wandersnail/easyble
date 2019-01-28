package com.snail.easyble.callback

import com.snail.easyble.core.*

/**
 *
 *
 * date: 2019/1/26 14:09
 * author: zengfansheng
 */
interface RequestFailedCallback {
    companion object {
        internal fun getMethodInfo(device: Device, tag: String, requestType: Request.RequestType, failType: Int, src: ByteArray?): MethodInfo {
            return MethodInfo("onRequestFailed", arrayOf(ValueTypePair(device, Device::class.java), ValueTypePair(tag, String::class.java),
                    ValueTypePair(requestType, Request.RequestType::class.java), ValueTypePair(failType, Int::class.java), ValueTypePair(src, ByteArray::class.java)))
        }
    }
    
    /**
     * @param failType One of [IConnection.REQUEST_FAIL_TYPE_REQUEST_FAILED],
     * [IConnection.REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC],
     * [IConnection.REQUEST_FAIL_TYPE_NULL_DESCRIPTOR],
     * [IConnection.REQUEST_FAIL_TYPE_NULL_SERVICE],
     * [IConnection.REQUEST_FAIL_TYPE_GATT_STATUS_FAILED],
     * [IConnection.REQUEST_FAIL_TYPE_GATT_IS_NULL],
     * [IConnection.REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW],
     * [IConnection.REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED] etc.
     */
    fun onRequestFailed(device: Device, tag: String, requestType: Request.RequestType, failType: Int, src: ByteArray?)
}