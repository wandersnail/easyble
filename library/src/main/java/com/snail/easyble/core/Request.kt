package com.snail.easyble.core

import android.support.annotation.IntRange
import com.snail.easyble.callback.RequestCallback
import com.snail.easyble.util.BleUtils
import java.util.*

/**
 * date: 2018/4/11 15:15
 * author: zengfansheng
 */
class Request private constructor(val type: RequestType, val requestId: String, val service: UUID?, val characteristic: UUID?, val descriptor: UUID?, var value: ByteArray?, 
                                  internal val callback: RequestCallback<*>?, val priority: Int = 0) : Comparable<Request> {
    internal var waitWriteResult = false
    internal var writeDelay = 0
    //-----used when packeting transmission-----
    internal var remainQueue: Queue<ByteArray>? = null
    internal var sendingBytes: ByteArray? = null

    enum class RequestType {
        ENABLE_NOTIFICATION, ENABLE_INDICATION, DISABLE_NOTIFICATION, DISABLE_INDICATION, READ_CHARACTERISTIC, READ_DESCRIPTOR, READ_RSSI, WRITE_CHARACTERISTIC, CHANGE_MTU, READ_PHY, SET_PREFERRED_PHY
    }

    override fun compareTo(other: Request): Int {
        //descending order
        return other.priority.compareTo(priority)
    }
    
    companion object {

        @JvmOverloads
        internal fun newChangeMtuRequest(requestId: String, @IntRange(from = 23, to = 517) mtu: Int, callback: RequestCallback<*>? = null, priority: Int): Request {
            return Request(RequestType.CHANGE_MTU, requestId, null, null, null, BleUtils.numberToBytes(false, mtu.toLong(), 4), callback, priority)
        }

        @JvmOverloads
        internal fun newReadCharacteristicRequest(requestId: String, service: UUID, characteristic: UUID, callback: RequestCallback<*>? = null, priority: Int): Request {
            return Request(RequestType.READ_CHARACTERISTIC, requestId, service, characteristic, null, null, callback, priority)
        }

        @JvmOverloads
        internal fun newEnableNotificationRequest(requestId: String, service: UUID, characteristic: UUID, callback: RequestCallback<*>? = null, priority: Int): Request {
            return Request(RequestType.ENABLE_NOTIFICATION, requestId, service, characteristic, null, null, callback, priority)
        }

        @JvmOverloads
        internal fun newDisableNotificationRequest(requestId: String, service: UUID, characteristic: UUID, callback: RequestCallback<*>? = null, priority: Int = 0): Request {
            return Request(RequestType.DISABLE_NOTIFICATION, requestId, service, characteristic, null, null, callback, priority)
        }

        @JvmOverloads
        internal fun newEnableIndicationRequest(requestId: String, service: UUID, characteristic: UUID, callback: RequestCallback<*>? = null, priority: Int): Request {
            return Request(RequestType.ENABLE_INDICATION, requestId, service, characteristic, null, null, callback, priority)
        }

        @JvmOverloads
        internal fun newDisableIndicationRequest(requestId: String, service: UUID, characteristic: UUID, callback: RequestCallback<*>? = null, priority: Int): Request {
            return Request(RequestType.DISABLE_INDICATION, requestId, service, characteristic, null, null, callback, priority)
        }

        @JvmOverloads
        internal fun newReadDescriptorRequest(requestId: String, service: UUID, characteristic: UUID, descriptor: UUID, callback: RequestCallback<*>? = null, priority: Int): Request {
            return Request(RequestType.READ_DESCRIPTOR, requestId, service, characteristic, descriptor, null, callback, priority)
        }

        @JvmOverloads
        internal fun newWriteCharacteristicRequest(requestId: String, service: UUID, characteristic: UUID, value: ByteArray, callback: RequestCallback<*>? = null, priority: Int): Request {
            return Request(RequestType.WRITE_CHARACTERISTIC, requestId, service, characteristic, null, value, callback, priority)
        }

        @JvmOverloads
        internal fun newReadRssiRequest(requestId: String, callback: RequestCallback<*>? = null, priority: Int): Request {
            return Request(RequestType.READ_RSSI, requestId, null, null, null, null, callback, priority)
        }

        @JvmOverloads
        internal fun newReadPhyRequest(requestId: String, callback: RequestCallback<*>? = null, priority: Int): Request {
            return Request(RequestType.READ_PHY, requestId, null, null, null, null, callback, priority)
        }

        @JvmOverloads
        internal fun newSetPreferredPhyRequest(requestId: String, txPhy: Int, rxPhy: Int, phyOptions: Int, callback: RequestCallback<*>? = null, priority: Int): Request {
            val tx = BleUtils.numberToBytes(false, txPhy.toLong(), 4)
            val rx = BleUtils.numberToBytes(false, rxPhy.toLong(), 4)
            val options = BleUtils.numberToBytes(false, phyOptions.toLong(), 4)
            val value = Arrays.copyOf(tx, 12)
            System.arraycopy(rx, 0, value, 4, 4)
            System.arraycopy(options, 0, value, 8, 4)
            return Request(RequestType.SET_PREFERRED_PHY, requestId, null, null, null, value, callback, priority)
        }
    }
}
