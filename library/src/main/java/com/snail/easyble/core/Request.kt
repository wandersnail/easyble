package com.snail.easyble.core

import androidx.annotation.IntRange
import com.snail.easyble.util.BleUtils
import java.util.*

/**
 * date: 2018/4/11 15:15
 * author: zengfansheng
 */
class Request private constructor(val type: RequestType, val tag: String, val service: UUID?, val characteristic: UUID?, val descriptor: UUID?, var value: ByteArray?, 
                                  internal val callback: Any?, val priority: Int = 0) : Comparable<Request> {
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
        internal fun newChangeMtuRequest(tag: String, @IntRange(from = 23, to = 517) mtu: Int, callback: Any? = null, priority: Int): Request {
            val value = if (mtu < 23) 23 else if (mtu > 517) 517 else mtu
            return Request(RequestType.CHANGE_MTU, tag, null, null, null, BleUtils.numberToBytes(false, value.toLong(), 4), callback, priority)
        }

        @JvmOverloads
        internal fun newReadCharacteristicRequest(tag: String, service: UUID, characteristic: UUID, callback: Any? = null, priority: Int): Request {
            return Request(RequestType.READ_CHARACTERISTIC, tag, service, characteristic, null, null, callback, priority)
        }

        @JvmOverloads
        internal fun newEnableNotificationRequest(tag: String, service: UUID, characteristic: UUID, callback: Any? = null, priority: Int): Request {
            return Request(RequestType.ENABLE_NOTIFICATION, tag, service, characteristic, null, null, callback, priority)
        }

        @JvmOverloads
        internal fun newDisableNotificationRequest(tag: String, service: UUID, characteristic: UUID, callback: Any? = null, priority: Int = 0): Request {
            return Request(RequestType.DISABLE_NOTIFICATION, tag, service, characteristic, null, null, callback, priority)
        }

        @JvmOverloads
        internal fun newEnableIndicationRequest(tag: String, service: UUID, characteristic: UUID, callback: Any? = null, priority: Int): Request {
            return Request(RequestType.ENABLE_INDICATION, tag, service, characteristic, null, null, callback, priority)
        }

        @JvmOverloads
        internal fun newDisableIndicationRequest(tag: String, service: UUID, characteristic: UUID, callback: Any? = null, priority: Int): Request {
            return Request(RequestType.DISABLE_INDICATION, tag, service, characteristic, null, null, callback, priority)
        }

        @JvmOverloads
        internal fun newReadDescriptorRequest(tag: String, service: UUID, characteristic: UUID, descriptor: UUID, callback: Any? = null, priority: Int): Request {
            return Request(RequestType.READ_DESCRIPTOR, tag, service, characteristic, descriptor, null, callback, priority)
        }

        @JvmOverloads
        internal fun newWriteCharacteristicRequest(tag: String, service: UUID, characteristic: UUID, value: ByteArray, callback: Any? = null, priority: Int): Request {
            return Request(RequestType.WRITE_CHARACTERISTIC, tag, service, characteristic, null, value, callback, priority)
        }

        @JvmOverloads
        internal fun newReadRssiRequest(tag: String, callback: Any? = null, priority: Int): Request {
            return Request(RequestType.READ_RSSI, tag, null, null, null, null, callback, priority)
        }

        @JvmOverloads
        internal fun newReadPhyRequest(tag: String, callback: Any? = null, priority: Int): Request {
            return Request(RequestType.READ_PHY, tag, null, null, null, null, callback, priority)
        }

        @JvmOverloads
        internal fun newSetPreferredPhyRequest(tag: String, txPhy: Int, rxPhy: Int, phyOptions: Int, callback: Any? = null, priority: Int): Request {
            val tx = BleUtils.numberToBytes(false, txPhy.toLong(), 4)
            val rx = BleUtils.numberToBytes(false, rxPhy.toLong(), 4)
            val options = BleUtils.numberToBytes(false, phyOptions.toLong(), 4)
            val value = tx.copyOf(12)
            System.arraycopy(rx, 0, value, 4, 4)
            System.arraycopy(options, 0, value, 8, 4)
            return Request(RequestType.SET_PREFERRED_PHY, tag, null, null, null, value, callback, priority)
        }
    }
}
