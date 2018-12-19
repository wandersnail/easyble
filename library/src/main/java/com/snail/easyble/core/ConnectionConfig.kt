package com.snail.easyble.core

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import java.util.*

/**
 * 描述:
 * 时间: 2018/8/5 19:28
 * 作者: zengfansheng
 */
class ConnectionConfig : Cloneable {
    /** 连接成功后，延时发现服务的时间 */
    var discoverServicesDelayMillis: Long = 500
        internal set
    /** 连接超时时间 */
    var connectTimeoutMillis = 10000
        private set
    /** 请求超时时间 */
    var requestTimeoutMillis = 3000
        private set
    /** 断开后尝试自动重连次数 */
    var tryReconnectTimes = TRY_RECONNECT_TIMES_INFINITE
        private set
    /** 包发送延时 */
    var packageWriteDelayMillis: Int = 0
        private set
    /** 写请求延时 */
    var requestWriteDelayMillis = -1
        private set
    /** 发送数据时的分包大小 */
    var packageSize = 20 
        private set
    /** 是否等待写入结果 */
    var isWaitWriteResult = true
        private set
    /** 不搜索，直接通过mac连接的最大连接次数 */
    var reconnectImmediatelyTimes = 3
        private set
    private val writeTypeMap = HashMap<String, Int>()
    /** 是否断线自动重连 */
    var isAutoReconnect = true
        private set
    /** 连接时的传输模式，只在6.0以上系统有效 */
    var transport = -1
        private set

    /**
     * 设置连接超时时间
     */
    fun setConnectTimeoutMillis(connectTimeoutMillis: Int): ConnectionConfig {
        this.connectTimeoutMillis = connectTimeoutMillis
        return this
    }

    /**
     * 设置连接成功后，延时发现服务的时间
     *
     * @param delayMillis 延时，毫秒
     */
    fun setDiscoverServicesDelayMillis(delayMillis: Int): ConnectionConfig {
        this.discoverServicesDelayMillis = delayMillis.toLong()
        return this
    }

    /**
     * 设置断开后尝试自动重连次数。-1为无限重连。默认为-1
     */
    fun setTryReconnectTimes(tryReconnectTimes: Int): ConnectionConfig {
        this.tryReconnectTimes = tryReconnectTimes
        return this
    }

    /**
     * 设置包发送延时，默认不延时
     */
    fun setPackageWriteDelayMillis(packageWriteDelayMillis: Int): ConnectionConfig {
        this.packageWriteDelayMillis = packageWriteDelayMillis
        return this
    }

    /**
     * 设置写请求延时，如写请求需要分包，每包发送时的延时是包延时，而非此延时。默认不延时
     */
    fun setRequestWriteDelayMillis(requestWriteDelayMillis: Int): ConnectionConfig {
        this.requestWriteDelayMillis = requestWriteDelayMillis
        return this
    }

    /**
     * 发送数据时的分包大小
     *
     * @param packageSize 包大小，字节
     */
    fun setPackageSize(packageSize: Int): ConnectionConfig {
        if (packageSize > 0) {
            this.packageSize = packageSize
        }
        return this
    }

    fun getWriteType(service: UUID, characteristic: UUID): Int? {
        return writeTypeMap[String.format(Locale.US, "%s:%s", service.toString(), characteristic.toString())]
    }

    /**
     * 设置写入模式，默认的规则：如果Characteristic的属性有PROPERTY_WRITE_NO_RESPONSE则使用
     * [BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE]，否则使用[BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT]
     *
     * @param writeType 写入模式。
     * - [BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE]
     * - [BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT]
     * - [BluetoothGattCharacteristic.WRITE_TYPE_SIGNED]
     */
    fun setWriteType(service: UUID, characteristic: UUID, writeType: Int): ConnectionConfig {
        writeTypeMap[String.format(Locale.US, "%s:%s", service.toString(), characteristic.toString())] = writeType
        return this
    }

    /**
     * 是否等待写入结果。不等待则直接处理下一个请求，否则等待onCharacteristicWrite回调后再处理下一请求，默认等待。
     * 不等待的话onCharacteristicWrite回调不会处理，而是在writeCharacteristic发布onCharacteristicWrite信息
     */
    fun setWaitWriteResult(isWaitWriteResult: Boolean): ConnectionConfig {
        this.isWaitWriteResult = isWaitWriteResult
        return this
    }

    /**
     * 连接失败或连接断开时，不搜索，直接通过mac尝试重连
     * @param reconnectImmediatelyTimes 最大尝试次数。超过此次数后进行搜索重连
     */
    fun setReconnectImmediatelyTimes(reconnectImmediatelyTimes: Int): ConnectionConfig {
        this.reconnectImmediatelyTimes = reconnectImmediatelyTimes
        return this
    }

    /**
     * 是否断线自动重连
     */
    fun setAutoReconnect(isAutoReconnect: Boolean): ConnectionConfig {
        this.isAutoReconnect = isAutoReconnect
        return this
    }

    /**
     * @param transport 连接时的传输模式，只在6.0以上系统有效。
     * - [BluetoothDevice.TRANSPORT_AUTO]
     * - [BluetoothDevice.TRANSPORT_BREDR]
     * - [BluetoothDevice.TRANSPORT_LE]
     */
    fun setTransport(transport: Int): ConnectionConfig {
        this.transport = transport
        return this
    }

    /**
     * 设置请求超时时间
     */
    fun setRequestTimeoutMillis(requestTimeoutMillis: Int): ConnectionConfig {
        if (requestTimeoutMillis > 1000) {
            this.requestTimeoutMillis = requestTimeoutMillis
        }
        return this
    }

    companion object {
        const val TRY_RECONNECT_TIMES_INFINITE = -1 //无限重连
    }
}
