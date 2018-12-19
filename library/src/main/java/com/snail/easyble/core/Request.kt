package com.snail.easyble.core

import android.bluetooth.BluetoothGattDescriptor
import com.snail.easyble.callback.RequestCallback
import com.snail.easyble.util.BleUtils
import java.util.*

/**
 * 描述: 用作请求队列
 * 时间: 2018/4/11 15:15
 * 作者: zengfansheng
 */
class Request private constructor(var type: RequestType, var requestId: String, var service: UUID?, var characteristic: UUID?, var descriptor: UUID?, var value: ByteArray?, 
                                  internal var callback: RequestCallback<*>?) {
    internal var waitWriteResult: Boolean = false
    internal var writeDelay: Int = 0
    //-----分包发送时用到-----
    internal var remainQueue: Queue<ByteArray>? = null
    internal var sendingBytes: ByteArray? = null

    enum class RequestType {
        TOGGLE_NOTIFICATION, TOGGLE_INDICATION, READ_CHARACTERISTIC, READ_DESCRIPTOR, READ_RSSI, WRITE_CHARACTERISTIC, CHANGE_MTU
    }

    companion object {

        @JvmOverloads
        internal fun newChangeMtuRequest(requestId: String, mtu: Int, callback: RequestCallback<*>? = null): Request {
            var targetMtu = mtu
            if (targetMtu < 23) {
                targetMtu = 23
            } else if (targetMtu > 517) {
                targetMtu = 517
            }
            return Request(RequestType.CHANGE_MTU, requestId, null, null, null, BleUtils.numberToBytes(false, targetMtu.toLong(), 4), callback)
        }

        @JvmOverloads
        internal fun newReadCharacteristicRequest(requestId: String, service: UUID, characteristic: UUID, callback: RequestCallback<*>? = null): Request {
            return Request(RequestType.READ_CHARACTERISTIC, requestId, service, characteristic, null, null, callback)
        }

        @JvmOverloads
        internal fun newToggleNotificationRequest(requestId: String, service: UUID, characteristic: UUID, enable: Boolean, callback: RequestCallback<*>? = null): Request {
            return Request(RequestType.TOGGLE_NOTIFICATION, requestId, service, characteristic, null,
                    if (enable) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, callback)
        }

        @JvmOverloads
        internal fun newToggleIndicationRequest(requestId: String, service: UUID, characteristic: UUID, enable: Boolean, callback: RequestCallback<*>? = null): Request {
            return Request(RequestType.TOGGLE_INDICATION, requestId, service, characteristic, null,
                    if (enable) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, callback)
        }

        @JvmOverloads
        internal fun newReadDescriptorRequest(requestId: String, service: UUID, characteristic: UUID, descriptor: UUID, callback: RequestCallback<*>? = null): Request {
            return Request(RequestType.READ_DESCRIPTOR, requestId, service, characteristic, descriptor, null, callback)
        }

        @JvmOverloads
        internal fun newWriteCharacteristicRequest(requestId: String, service: UUID, characteristic: UUID, value: ByteArray, callback: RequestCallback<*>? = null): Request {
            return Request(RequestType.WRITE_CHARACTERISTIC, requestId, service, characteristic, null, value, callback)
        }

        @JvmOverloads
        internal fun newReadRssiRequest(requestId: String, callback: RequestCallback<*>? = null): Request {
            return Request(RequestType.READ_RSSI, requestId, null, null, null, null, callback)
        }
    }
}
