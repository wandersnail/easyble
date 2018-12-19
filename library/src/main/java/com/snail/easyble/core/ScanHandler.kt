package com.snail.easyble.core

import android.bluetooth.BluetoothDevice
import java.util.*
import kotlin.collections.ArrayList

/**
 * 描述: 蓝牙搜索过滤器
 * 时间: 2018/4/13 10:42
 * 作者: zengfansheng
 */
abstract class ScanHandler private constructor() {
    internal val uuids = ArrayList<UUID>()
    internal val names = ArrayList<String>()
    internal val addrs = ArrayList<String>()

    /**
     * 添加过滤的
     */
    fun addAcceptNames(names: List<String>): ScanHandler {
        this.names.removeAll(names)
        this.names.addAll(names)
        return this
    }

    fun addAcceptAddrs(addrs: List<String>): ScanHandler {
        this.addrs.removeAll(addrs)
        this.addrs.addAll(addrs)
        return this
    }

    fun addAcceptServiceUuid(uuids: List<UUID>): ScanHandler {
        this.uuids.removeAll(uuids)
        this.uuids.addAll(uuids)
        return this
    }
    
    /**
     * 根据广播信息添加设备属性并确定是否过滤
     * @param device 搜索到的设备
     * @param advData 广播内容
     * @return 是否过滤，null时表示不过滤
     */
    abstract fun handleAdvertisingData(device: BluetoothDevice, advData: ByteArray): Device?
}
