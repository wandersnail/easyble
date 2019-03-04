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
    /** 连接成功后延时多久开始执行发现服务 */
    var discoverServicesDelayMillis = 500
        internal set
    /** 连接超时时长 */
    var connectTimeoutMillis = 10000
        private set
    /** 请求超时时长 */
    var requestTimeoutMillis = 3000
        private set
    /** 最大尝试自动重连次数 */
    var tryReconnectTimes = TRY_RECONNECT_TIMES_INFINITE
        private set
    /** 两次写数据到特征的时间间隔 */
    var packageWriteDelayMillis = 0
        private set
    /** 两次写请求的时间间隔，和[packageWriteDelayMillis]不同的是，一次写请求可能会分包发送。
     * 一个是请求与请求的间隔，一个是包与包的间隔 */
    var requestWriteDelayMillis = -1
        private set
    /** 一次向特征写入的字节数 */
    var packageSize = 20 
        private set
    /** 是否等待写入结果回调 */
    var isWaitWriteResult = true
        private set
    /** 不经过搜索，直接使用之间的MAC地址连接的次数，重连达到此次数后，恢复搜索到设备再进行连接 */
    var reconnectImmediatelyTimes = 3
        private set
    private val writeTypeMap = HashMap<String, Int>()
    /** 是否自动重连 */
    var isAutoReconnect = true
        private set
    /** 双模蓝牙的传输模式，[BluetoothDevice.TRANSPORT_AUTO] or
     * [BluetoothDevice.TRANSPORT_BREDR] or [BluetoothDevice.TRANSPORT_LE] */
    @RequiresApi(Build.VERSION_CODES.M)
    var transport = BluetoothDevice.TRANSPORT_LE
        private set
    /** 物理层的模式 */
    @RequiresApi(Build.VERSION_CODES.O)
    var phy = BluetoothDevice.PHY_LE_1M_MASK
        private set

    /**
     * 设置连接超时时长
     */
    fun setConnectTimeoutMillis(connectTimeoutMillis: Int): ConnectionConfig {
        this.connectTimeoutMillis = connectTimeoutMillis
        return this
    }

    /**
     * 设置连接成功后延时多久开始执行发现服务
     */
    fun setDiscoverServicesDelayMillis(delayMillis: Int): ConnectionConfig {
        this.discoverServicesDelayMillis = delayMillis
        return this
    }

    /**
     * 最大尝试自动重连次数. 默认是无限重连[TRY_RECONNECT_TIMES_INFINITE]
     */
    fun setTryReconnectTimes(tryReconnectTimes: Int): ConnectionConfig {
        this.tryReconnectTimes = tryReconnectTimes
        return this
    }

    /**
     * 设置两次写数据到到特征的时间间隔
     */
    fun setPackageWriteDelayMillis(packageWriteDelayMillis: Int): ConnectionConfig {
        this.packageWriteDelayMillis = packageWriteDelayMillis
        return this
    }

    /**
     * 设置两次写请求的时间间隔，和[setPackageWriteDelayMillis]不同的是，一次写请求可能会分包发送。
     * 一个是请求与请求的间隔，一个是包与包的间隔
     */
    fun setRequestWriteDelayMillis(requestWriteDelayMillis: Int): ConnectionConfig {
        this.requestWriteDelayMillis = requestWriteDelayMillis
        return this
    }

    /**
     * 设置包大小。此大小为自动分包的大小，当写请求的数据长度大小此大小时，会自动分包后发送
     */
    fun setPackageSize(packageSize: Int): ConnectionConfig {
        if (packageSize > 0) {
            this.packageSize = packageSize
        }
        return this
    }

    /**
     * 获取特征的写入模式
     */
    fun getWriteType(service: UUID, characteristic: UUID): Int? {
        return writeTypeMap[String.format(Locale.US, "%s:%s", service.toString(), characteristic.toString())]
    }

    /**
     * 设置特征的写入模式
     *
     * @param writeType [BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE],
     * [BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT], [BluetoothGattCharacteristic.WRITE_TYPE_SIGNED]
     */
    fun setWriteType(service: UUID, characteristic: UUID, writeType: Int): ConnectionConfig {
        writeTypeMap[String.format(Locale.US, "%s:%s", service.toString(), characteristic.toString())] = writeType
        return this
    }

    /**
     * 设置是否等待写入结果回调
     * @param isWaitWriteResult true时，则在onCharacteristicWrite回调后再送下一包数据，否则直接发下包数据
     */
    fun setWaitWriteResult(isWaitWriteResult: Boolean): ConnectionConfig {
        this.isWaitWriteResult = isWaitWriteResult
        return this
    }

    /**
     * 设置不经过搜索，直接使用之间的MAC地址连接的次数，重连达到此次数后，恢复搜索到设备再进行连接
     */
    fun setReconnectImmediatelyTimes(reconnectImmediatelyTimes: Int): ConnectionConfig {
        this.reconnectImmediatelyTimes = reconnectImmediatelyTimes
        return this
    }

    /**
     * 设置是否自动重连
     */
    fun setAutoReconnect(isAutoReconnect: Boolean): ConnectionConfig {
        this.isAutoReconnect = isAutoReconnect
        return this
    }

    /**
     * @param transport 设置双模蓝牙的传输模式。 [BluetoothDevice.TRANSPORT_AUTO] or 
     * [BluetoothDevice.TRANSPORT_BREDR] or [BluetoothDevice.TRANSPORT_LE]
     */
    fun setTransport(transport: Int): ConnectionConfig {
        this.transport = transport
        return this
    }

    /**
     * 设置请求超时时长
     */
    fun setRequestTimeoutMillis(requestTimeoutMillis: Int): ConnectionConfig {
        if (requestTimeoutMillis > 1000) {
            this.requestTimeoutMillis = requestTimeoutMillis
        }
        return this
    }

    /**
     * @param phy 设置物理层的模式，[BluetoothDevice.PHY_LE_1M_MASK], 
     * [BluetoothDevice.PHY_LE_2M_MASK], and [BluetoothDevice.PHY_LE_CODED_MASK]. 
     */
    fun setPhy(phy: Int): ConnectionConfig {
        this.transport = transport
        return this
    }

    companion object {
        /** 无限重连 */
        const val TRY_RECONNECT_TIMES_INFINITE = -1
    }
}
