package com.snail.easyble.core

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import com.snail.easyble.callback.ConnectionStateChangeListener
import com.snail.easyble.callback.ScanListener
import com.snail.easyble.event.Events
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 描述: 蓝牙操作
 * 时间: 2018/4/11 15:29
 * 作者: zengfansheng
 */
class Ble private constructor() {
    var bluetoothAdapter: BluetoothAdapter? = null
        private set
    private val connectionMap: MutableMap<String, Connection>
    private var isInited: Boolean = false
    
    /**
     * 配置
     */
    var bleConfig = BleConfig()    
    private val mainThreadHandler: Handler
    private val publisher: EventBus
    private val logger: BleLogger
    private val executorService: ExecutorService
    private var app: Application? = null
    private var scanner: Scanner? = null

    init {
        connectionMap = ConcurrentHashMap()
        mainThreadHandler = Handler(Looper.getMainLooper())        
        publisher = EventBus.builder().build()
        logger = BleLogger()
        executorService = Executors.newCachedThreadPool()
        mainThreadHandler.post { tryGetContext() }
    }
    
    internal val context: Context?
        get() {
            if (app == null) {
                tryGetContext()
                if (app != null) {
                    initialize(app!!)
                }
            }
            return app
        }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED == intent.action) { //蓝牙开关状态变化                 
                if (bluetoothAdapter != null) {
                    publisher.post(Events.newBluetoothStateChanged(bluetoothAdapter!!.state))
                    if (bluetoothAdapter!!.state == BluetoothAdapter.STATE_OFF) { //蓝牙关闭了
                        scanner?.onBluethoothOff()
                        //主动断开
                        connectionMap.values.forEach { 
                            it.disconnect()
                        }
                    } else if (bluetoothAdapter!!.state == BluetoothAdapter.STATE_ON) {
                        connectionMap.values.forEach {
                            if (it.isAutoReconnectEnabled) {
                                it.reconnect() //如果开启了自动重连，则重连
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 是否已初始化过或上下文为空了，需要重新初始化了
     */
    val isInitialized: Boolean
        get() = isInited && app != null

    val isBluetoothAdapterEnabled: Boolean
        get() = bluetoothAdapter != null && bluetoothAdapter!!.isEnabled

    internal object Holder {
        internal val BLE = Ble()
    }

    private fun tryGetContext() {
        try {
            val clazz = Class.forName("android.app.ActivityThread")
            val acThreadMethod = clazz.getMethod("currentActivityThread")
            acThreadMethod.isAccessible = true
            val acThread = acThreadMethod.invoke(null)
            val appMethod = acThread.javaClass.getMethod("getApplication")
            app = appMethod.invoke(acThread) as Application
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 设置日志输出级别控制，与[.setLogPrintFilter]同时作用
     *
     * @param logPrintLevel [BleLogger.NONE], [BleLogger.VERBOSE], [BleLogger.DEBUG], [BleLogger.INFO], [BleLogger.WARN], [BleLogger.ERROR]
     */
    fun setLogPrintLevel(logPrintLevel: Int) {
        logger.setPrintLevel(logPrintLevel)
    }

    /**
     * 设置日志输出过滤器，与[.setLogPrintLevel]同时作用
     */
    fun setLogPrintFilter(filter: BleLogger.Filter) {
        logger.setFilter(filter)
    }

    /**
     * 必须先初始化，只需一次
     *
     * @param app 上下文
     */
    @Synchronized
    fun initialize(app: Application): Boolean {
        if (isInited) {
            return true
        }
        //检查手机是否支持BLE
        if (!app.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false
        }
        //获取蓝牙管理器
        val bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (bluetoothManager == null || bluetoothManager.adapter == null) {
            return false
        }
        bluetoothAdapter = bluetoothManager.adapter
        //监听蓝牙开关变化
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        app.registerReceiver(receiver, filter)
        isInited = true
        scanner = Scanner(bluetoothAdapter!!, mainThreadHandler, bleConfig)
        return true
    }

    @Synchronized
    private fun checkInitStateAndContext(): Boolean {
        if (!isInited) {
            if (!tryAutoInit()) {
                Exception("The SDK has not been initialized, make sure to call Ble.getInstance().initialize(Application) first.").printStackTrace()
                return false
            }
        } else if (app == null) {
            return tryAutoInit()
        }
        return true
    }

    private fun tryAutoInit(): Boolean {
        tryGetContext()
        if (app != null) {
            initialize(app!!)
        }
        return isInited && app != null
    }

    /**
     * 关闭所有连接，释放资源
     */
    fun release() {
        if (checkInitStateAndContext()) {
            scanner?.release()
            releaseAllConnections() //释放所有连接
        }
    }

    fun postEvent(event: Any) {
        publisher.post(event)
    }

    fun postEventOnBackground(event: Any) {
        executorService.execute(EventRunnable(event))
    }

    private inner class EventRunnable internal constructor(private val event: Any) : Runnable {
        override fun run() {
            publisher.post(event)
        }
    }

    /**
     * 注册订阅者，开始监听蓝牙状态及数据
     *
     * @param subscriber 订阅者
     */
    fun registerSubscriber(subscriber: Any) {
        if (!publisher.isRegistered(subscriber)) {
            publisher.register(subscriber)
        }
    }

    /**
     * 取消注册订阅者，停止监听蓝牙状态及数据
     *
     * @param subscriber 订阅者
     */
    fun unregisterSubscriber(subscriber: Any) {
        publisher.unregister(subscriber)
    }

    /**
     * 添加扫描监听器
     */
    fun addScanListener(listener: ScanListener?) {
        scanner?.addScanListener(listener)
    }

    /**
     * 移除扫描监听器
     */
    fun removeScanListener(listener: ScanListener) {
        scanner?.removeScanListener(listener)
    }

    /**
     * 搜索蓝牙设备
     */
    fun startScan() {
        if (!checkInitStateAndContext()) {
            scanner?.startScan(app!!) { device, _, _ ->
                for (connection in connectionMap.values) {
                    connection.onScanResult(device.address)
                }
            }
        }
    }

    /**
     * 停止搜索蓝牙设备
     */
    fun stopScan() {
        if (!checkInitStateAndContext()) {
            return
        }
        scanner?.stopScan()
    }

    /**
     * 建立连接
     */
    @Synchronized
    fun connect(addr: String, config: ConnectionConfig, listener: ConnectionStateChangeListener?) {
        if (!checkInitStateAndContext()) {
            return
        }
        val device = bluetoothAdapter!!.getRemoteDevice(addr)
        if (device == null) {
            Connection.notifyConnectFailed(null, IConnection.CONNECT_FAIL_TYPE_UNSPECIFIED_MAC_ADDRESS, listener)
        } else {
            connect(Device.valueOf(device), config, listener)
        }
    }

    /**
     * 建立连接
     */
    @Synchronized
    fun connect(device: Device, config: ConnectionConfig, listener: ConnectionStateChangeListener?) {
        if (!checkInitStateAndContext()) {
            return
        }
        var connection = connectionMap.remove(device.addr)
        //此前这个设备建立过连接，销毁之前的连接重新创建
        connection?.releaseNoEvnet()
        if (device.isConnectable == null || device.isConnectable!!) {//可连接的
            val bondController = bleConfig.bondController
            connection = if (bondController != null && bondController.bond(device)) {
                val bd = bluetoothAdapter!!.getRemoteDevice(device.addr)
                if (bd.bondState == BluetoothDevice.BOND_BONDED) {
                    Connection.newInstance(bluetoothAdapter!!, device, config, 0, listener)
                } else {
                    createBond(device.addr) //配对
                    Connection.newInstance(bluetoothAdapter!!, device, config, 1500, listener)
                }
            } else {
                Connection.newInstance(bluetoothAdapter!!, device, config, 0, listener)
            }
            if (connection != null) {
                connectionMap[device.addr] = connection
            }
        } else {//不可连接
            listener?.onConnectFailed(device, IConnection.CONNECT_FAIL_TYPE_NON_CONNECTABLE)
        }
    }

    /**
     * 获取连接
     */
    fun getConnection(device: Device?): Connection? {
        return if (device == null) null else connectionMap[device.addr]
    }

    /**
     * 获取连接
     */
    fun getConnection(addr: String?): Connection? {
        return if (addr == null) null else connectionMap[addr]
    }

    /**
     * @return 连接状态
     * - [IConnection.STATE_DISCONNECTED]
     * - [IConnection.STATE_CONNECTING]
     * - [IConnection.STATE_SCANNING]
     * - [IConnection.STATE_CONNECTED]
     * - [IConnection.STATE_SERVICE_DISCOVERING]
     * - [IConnection.STATE_SERVICE_DISCOVERED]
     */
    fun getConnectionState(device: Device): Int {
        val connection = getConnection(device)
        return connection?.connctionState ?: IConnection.STATE_DISCONNECTED
    }

    /**
     * 根据设备断开其连接
     */
    fun disconnectConnection(device: Device?) {
        if (checkInitStateAndContext() && device != null) {
            connectionMap[device.addr]?.disconnect()
        }
    }

    /**
     * 断开所有连接
     */
    fun disconnectAllConnection() {
        connectionMap.values.forEach { it.disconnect() }
    }

    /**
     * 释放所有创建的连接
     */
    @Synchronized
    fun releaseAllConnections() {
        connectionMap.values.forEach { it.release() }
        connectionMap.clear()
    }

    /**
     * 根据设备释放连接
     */
    @Synchronized
    fun releaseConnection(device: Device?) {
        if (checkInitStateAndContext() && device != null) {
            connectionMap.remove(device.addr)?.release()
        }
    }

    /**
     * 重连所有创建的连接
     */
    fun reconnectAll() {
        if (checkInitStateAndContext()) {
            connectionMap.values.forEach {
                if (it.connctionState != IConnection.STATE_SERVICE_DISCOVERED) {
                    it.reconnect()
                }
            }
        }
    }

    /**
     * 根据设备重连
     */
    fun reconnect(device: Device?) {
        if (checkInitStateAndContext() && device != null) {
            val connection = connectionMap[device.addr]
            if (connection != null && connection.connctionState != IConnection.STATE_SERVICE_DISCOVERED) {
                connection.reconnect()
            }
        }
    }

    /**
     * 设置是否可自动重连，所有已创建的连接
     */
    fun setAutoReconnectEnable(enable: Boolean) {
        connectionMap.values.forEach { it.setAutoReconnectEnable(enable) }
    }

    /**
     * 设置是否可自动重连
     */
    fun setAutoReconnectEnable(device: Device?, enable: Boolean) {
        if (device != null) {
            connectionMap[device.addr]?.setAutoReconnectEnable(enable)
        }
    }

    /**
     * 刷新设备，清除缓存
     */
    fun refresh(device: Device?) {
        if (checkInitStateAndContext() && device != null) {
            connectionMap[device.addr]?.refresh()
        }
    }

    /**
     * 获取设备配对状态
     *
     * @return [BluetoothDevice.BOND_NONE],
     * [BluetoothDevice.BOND_BONDING],
     * [BluetoothDevice.BOND_BONDED].
     */
    fun getBondState(addr: String): Int {
        try {
            return bluetoothAdapter!!.getRemoteDevice(addr).bondState
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return BluetoothDevice.BOND_NONE
    }

    /**
     * 绑定设备
     */
    fun createBond(addr: String?): Boolean {
        try {
            val device = bluetoothAdapter!!.getRemoteDevice(addr)
            return device.bondState != BluetoothDevice.BOND_NONE || device.createBond()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    /**
     * 根据设置的过滤器清除已配对的设备
     */
    fun clearBondDevices(filter: RemoveBondFilter?) {
        val devices = bluetoothAdapter!!.bondedDevices
        for (device in devices) {
            if (filter == null || filter.accept(device)) {
                try {
                    device.javaClass.getMethod("removeBond").invoke(device)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 取消配对
     */
    fun removeBond(addr: String) {
        try {
            val device = bluetoothAdapter!!.getRemoteDevice(addr)
            if (device.bondState != BluetoothDevice.BOND_NONE) {
                device.javaClass.getMethod("removeBond").invoke(device)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {

        val instance: Ble
            get() = Holder.BLE

        fun println(cls: Class<*>, priority: Int, msg: String) {
            Ble.instance.postEvent(Events.newLogChanged(msg, BleLogger.getLevel(priority)))
            Ble.instance.logger.println("blelib:" + cls.simpleName, priority, msg)
        }
    }
}
