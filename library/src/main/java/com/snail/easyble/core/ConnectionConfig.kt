package com.snail.easyble.core

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.*

/**
 * 
 * date: 2018/8/5 19:28
 * author: zengfansheng
 */
class ConnectionConfig : Cloneable {
    /** The milliseconds of delay to discover remote services */
    var discoverServicesDelayMillis = 500
        internal set
    /** The milliseconds of connection timeout */
    var connectTimeoutMillis = 10000
        private set
    /** The milliseconds of request timeout */
    var requestTimeoutMillis = 3000
        private set
    /** Maximum number of attempts to reconnect */
    var tryReconnectTimes = TRY_RECONNECT_TIMES_INFINITE
        private set
    /** Interval of write characteristic between every package */
    var packageWriteDelayMillis = 0
        private set
    /** Interval of write characteristic between every write request */
    var requestWriteDelayMillis = -1
        private set
    /** The bytes of a package when write to characteristic one time */
    var packageSize = 20 
        private set
    /** Whether to wait the callback onCharacteristicWrite */
    var isWaitWriteResult = true
        private set
    /** Maximum number of reconnection times without scanning */
    var reconnectImmediatelyTimes = 3
        private set
    private val writeTypeMap = HashMap<String, Int>()
    /** Whether to automatically connect the remote device when disconnected */
    var isAutoReconnect = true
        private set
    /** preferred transport for GATT connections to remote dual-mode devices [BluetoothDevice.TRANSPORT_AUTO] or
     * [BluetoothDevice.TRANSPORT_BREDR] or [BluetoothDevice.TRANSPORT_LE] */
    @RequiresApi(Build.VERSION_CODES.M)
    var transport = BluetoothDevice.TRANSPORT_LE
        private set
    @RequiresApi(Build.VERSION_CODES.O)
    var phy = BluetoothDevice.PHY_LE_1M_MASK
        private set

    /**
     * Set the milliseconds of connection timeout
     */
    fun setConnectTimeoutMillis(connectTimeoutMillis: Int): ConnectionConfig {
        this.connectTimeoutMillis = connectTimeoutMillis
        return this
    }

    /**
     * Set the milliseconds of delay to discover remote services
     */
    fun setDiscoverServicesDelayMillis(delayMillis: Int): ConnectionConfig {
        this.discoverServicesDelayMillis = delayMillis
        return this
    }

    /**
     * Set the maximum number of attempts to reconnect. The default is [TRY_RECONNECT_TIMES_INFINITE]
     */
    fun setTryReconnectTimes(tryReconnectTimes: Int): ConnectionConfig {
        this.tryReconnectTimes = tryReconnectTimes
        return this
    }

    /**
     * Set interval of write characteristic between every package. Default no delay.
     */
    fun setPackageWriteDelayMillis(packageWriteDelayMillis: Int): ConnectionConfig {
        this.packageWriteDelayMillis = packageWriteDelayMillis
        return this
    }

    /**
     * Set the interval of write characteristic between every write request. Default no delay.
     */
    fun setRequestWriteDelayMillis(requestWriteDelayMillis: Int): ConnectionConfig {
        this.requestWriteDelayMillis = requestWriteDelayMillis
        return this
    }

    /**
     * Set the bytes of a package when write to characteristic one time
     */
    fun setPackageSize(packageSize: Int): ConnectionConfig {
        if (packageSize > 0) {
            this.packageSize = packageSize
        }
        return this
    }

    /**
     * Get the characteristic's write type
     */
    fun getWriteType(service: UUID, characteristic: UUID): Int? {
        return writeTypeMap[String.format(Locale.US, "%s:%s", service.toString(), characteristic.toString())]
    }

    /**
     * Set characteristic's write type
     *
     * @param writeType One of [BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE],
     * [BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT], [BluetoothGattCharacteristic.WRITE_TYPE_SIGNED]
     */
    fun setWriteType(service: UUID, characteristic: UUID, writeType: Int): ConnectionConfig {
        writeTypeMap[String.format(Locale.US, "%s:%s", service.toString(), characteristic.toString())] = writeType
        return this
    }

    /**
     * Set whether to wait the callback onCharacteristicWrite
     */
    fun setWaitWriteResult(isWaitWriteResult: Boolean): ConnectionConfig {
        this.isWaitWriteResult = isWaitWriteResult
        return this
    }

    /**
     * Set the maximum number of reconnection times without scanning
     */
    fun setReconnectImmediatelyTimes(reconnectImmediatelyTimes: Int): ConnectionConfig {
        this.reconnectImmediatelyTimes = reconnectImmediatelyTimes
        return this
    }

    /**
     * Set whether to automatically connect the remote device when disconnected
     */
    fun setAutoReconnect(isAutoReconnect: Boolean): ConnectionConfig {
        this.isAutoReconnect = isAutoReconnect
        return this
    }

    /**
     * @param transport preferred transport for GATT connections to remote dual-mode devices [BluetoothDevice.TRANSPORT_AUTO] or 
     * [BluetoothDevice.TRANSPORT_BREDR] or [BluetoothDevice.TRANSPORT_LE]
     */
    fun setTransport(transport: Int): ConnectionConfig {
        this.transport = transport
        return this
    }

    /**
     * Set the milliseconds of request timeout
     */
    fun setRequestTimeoutMillis(requestTimeoutMillis: Int): ConnectionConfig {
        if (requestTimeoutMillis > 1000) {
            this.requestTimeoutMillis = requestTimeoutMillis
        }
        return this
    }

    /**
     * @param phy preferred PHY for connections to remote LE device. Bitwise OR of any of [BluetoothDevice.PHY_LE_1M_MASK], 
     * [BluetoothDevice.PHY_LE_2M_MASK], and [BluetoothDevice.PHY_LE_CODED_MASK]. 
     */
    fun setPhy(phy: Int): ConnectionConfig {
        this.transport = transport
        return this
    }

    companion object {
        /** reconnect forever */
        const val TRY_RECONNECT_TIMES_INFINITE = -1
    }
}
