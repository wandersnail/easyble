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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The Ble is responsible for managing scanner and connections.
 * 
 * date: 2018/4/11 15:29
 * author: zengfansheng
 */
class Ble private constructor() {
    var bluetoothAdapter: BluetoothAdapter? = null
        private set
    private val connectionMap: MutableMap<String, Connection>
    private var isInited: Boolean = false
    var bleConfig = BleConfig()    
    private val mainHandler: Handler
    private val logger: BleLogger
    private val executorService: ExecutorService
    private var app: Application? = null
    private var scanner: Scanner? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private val methodPoster: MethodPoster

    init {
        connectionMap = ConcurrentHashMap()
        mainHandler = Handler(Looper.getMainLooper())
        logger = BleLogger()
        executorService = Executors.newCachedThreadPool()
        methodPoster = MethodPoster(executorService, mainHandler)
        mainHandler.post { tryGetContext() }
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

    private inner class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED == intent.action) { //bluetooth state change                 
                if (bluetoothAdapter != null) {
                    getObservable().notifyBluetoothStateChanged(bluetoothAdapter!!.state)
                    if (bluetoothAdapter!!.state == BluetoothAdapter.STATE_OFF) { //bluetooth off
                        scanner?.onBluethoothOff()
                        //disconnect all connections
                        connectionMap.values.forEach { 
                            it.disconnect()
                        }
                    } else if (bluetoothAdapter!!.state == BluetoothAdapter.STATE_ON) {
                        connectionMap.values.forEach {
                            if (it.isAutoReconnectEnabled) {
                                it.reconnect() //reconnect if isAutoReconnectEnabled is true
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * return false if uninitialized or app == null
     */
    val isInitialized: Boolean
        get() = isInited && app != null

    val isBluetoothAdapterEnabled: Boolean
        get() = bluetoothAdapter != null && bluetoothAdapter!!.isEnabled

    internal fun getObservable() = bleConfig.eventObservable

    internal fun getMethodPoster() = methodPoster

    internal object Holder {
        internal val BLE = Ble()
    }

    //Try using reflection to get an instance of Application
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
     * Set log output level. See [setLogPrintFilter]
     *
     * @param logPrintLevel One of [BleLogger.NONE], [BleLogger.VERBOSE], [BleLogger.DEBUG], [BleLogger.INFO], [BleLogger.WARN], [BleLogger.ERROR]
     */
    fun setLogPrintLevel(logPrintLevel: Int) {
        logger.setPrintLevel(logPrintLevel)
    }

    /**
     * Set the log output filter. See [setLogPrintLevel]
     */
    fun setLogPrintFilter(filter: BleLogger.Filter) {
        logger.setFilter(filter)
    }

    /**
     * Must be initialized before using the SDK
     */
    @Synchronized
    fun initialize(app: Application): Boolean {
        if (isInitialized) {
            return true
        }
        this.app = app
        //Check if the phone supports BLE
        if (!app.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false
        }
        //Get a Bluetooth adapter,
        val bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (bluetoothManager == null || bluetoothManager.adapter == null) {
            return false
        }
        bluetoothAdapter = bluetoothManager.adapter
        //Register Bluetooth status change BroadcastReceiver to listen changes 
        if (broadcastReceiver == null) {
            broadcastReceiver = MyBroadcastReceiver()
            val filter = IntentFilter()
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            app.registerReceiver(broadcastReceiver, filter)
        }
        isInited = true
        scanner = Scanner(bluetoothAdapter!!, mainHandler)
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
     * close all active connections and release resources
     */
    @Synchronized
    fun release() {
        if (broadcastReceiver != null) {
            app?.unregisterReceiver(broadcastReceiver)
            broadcastReceiver = null
        }
        isInited = false
        scanner?.release()
        releaseAllConnections()
        getObservable().unregisterAll()
    }
    
    /**
     * Subscribe events of bluetooth status change and receive data and requests result etc. See [unregisterObserver]
     */
    fun registerObserver(observer: EventObserver) {
        if (!getObservable().isRegistered(observer)) {
            getObservable().registerObserver(observer)
        }
    }
    
    fun isObserverRegistered(observer: EventObserver): Boolean {
        return getObservable().isRegistered(observer)
    }

    /**
     * Unsubscribe events of bluetooth status change and receive data and requests result etc. See [registerObserver]
     */
    fun unregisterObserver(observer: EventObserver) {
        if (getObservable().isRegistered(observer)) {
            getObservable().unregisterObserver(observer)
        }
    }
    
    /**
     * 添加搜索监听器
     */
    fun addScanListener(listener: ScanListener?) {
        scanner?.addScanListener(listener)
    }

    /**
     * 移除搜索监听器
     */
    fun removeScanListener(listener: ScanListener) {
        scanner?.removeScanListener(listener)
    }

    /**
     * 搜索BLE设备
     */
    fun startScan() {
        if (checkInitStateAndContext()) {
            scanner?.startScan(app!!) { device, _, _ ->
                for (connection in connectionMap.values) {
                    connection.onScanResult(device.address)
                }
            }
        }
    }

    /**
     * 停止搜索
     */
    fun stopScan() {
        if (checkInitStateAndContext()) {
            scanner?.stopScan()
        }        
    }

    /**
     * 建立连接
     */
    @Synchronized
    fun connect(addr: String, config: ConnectionConfig, listener: ConnectionStateChangeListener?): Connection? {
        if (!checkInitStateAndContext()) {
            return null
        }
        val device = bluetoothAdapter!!.getRemoteDevice(addr)
        return if (device == null) {
            Connection.notifyConnectFailed(null, IConnection.CONNECT_FAIL_TYPE_UNSPECIFIED_ADDRESS, listener)
            null
        } else {
            connect(Device(device), config, listener)
        }
    }

    /**
     * 建立连接
     */
    @Synchronized
    fun connect(device: Device, config: ConnectionConfig, listener: ConnectionStateChangeListener?): Connection? {
        if (!checkInitStateAndContext()) {
            return null
        }
        var connection = connectionMap.remove(device.addr)
        //if the connection exists, release it
        connection?.releaseNoEvnet()
        if (device.isConnectable == null || device.isConnectable!!) {
            val bondController = bleConfig.bondController
            connection = if (bondController != null && bondController.bond(device)) {
                val bd = bluetoothAdapter!!.getRemoteDevice(device.addr)
                if (bd.bondState == BluetoothDevice.BOND_BONDED) {
                    Connection.newInstance(bluetoothAdapter!!, device, config, 0, listener)
                } else {
                    createBond(device.addr)
                    Connection.newInstance(bluetoothAdapter!!, device, config, 1500, listener)
                }
            } else {
                Connection.newInstance(bluetoothAdapter!!, device, config, 0, listener)
            }
            if (connection != null) {
                connectionMap[device.addr] = connection
                return connection
            }
        } else {
            listener?.onConnectFailed(device, IConnection.CONNECT_FAIL_TYPE_NON_CONNECTABLE)
        }
        return null
    }

    /**
     * 获取设备的连接实例
     */
    fun getConnection(device: Device?): Connection? {
        return if (device == null) null else connectionMap[device.addr]
    }

    /**
     * 获取设备的连接实例
     */
    fun getConnection(addr: String?): Connection? {
        return if (addr == null) null else connectionMap[addr]
    }

    /**
     * 获取设备连接状态
     * 
     * @return [IConnection.STATE_DISCONNECTED], [IConnection.STATE_CONNECTING], [IConnection.STATE_SCANNING],
     * [IConnection.STATE_CONNECTED], [IConnection.STATE_SERVICE_DISCOVERING], [IConnection.STATE_SERVICE_DISCOVERED]
     */
    fun getConnectionState(device: Device): Int {
        val connection = getConnection(device)
        return connection?.connctionState ?: IConnection.STATE_DISCONNECTED
    }

    /**
     * 断开连接
     */
    fun disconnectConnection(device: Device?) {
        if (checkInitStateAndContext() && device != null) {
            connectionMap[device.addr]?.disconnect()
        }
    }

    /**
     * 断开所有连接
     */
    fun disconnectAllConnections() {
        connectionMap.values.forEach { it.disconnect() }
    }

    /**
     * 释放所有连接
     */
    @Synchronized
    fun releaseAllConnections() {
        connectionMap.values.forEach { it.release() }
        connectionMap.clear()
    }

    /**
     * 释放连接
     */
    @Synchronized
    fun releaseConnection(device: Device?) {
        if (checkInitStateAndContext() && device != null) {
            connectionMap.remove(device.addr)?.release()
        }
    }

    /**
     * 重连所有设备
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
     * 重连设备
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
     * 设置是否断线重连，作用于所有连接
     */
    fun setAutoReconnectEnable(enable: Boolean) {
        connectionMap.values.forEach { it.setAutoReconnectEnable(enable) }
    }

    /**
     * 设置是否断线重连
     */
    fun setAutoReconnectEnable(device: Device?, enable: Boolean) {
        if (device != null) {
            connectionMap[device.addr]?.setAutoReconnectEnable(enable)
        }
    }

    /**
     * Clears the internal cache and forces a refresh of the services from the remote device.
     */
    fun refresh(device: Device?) {
        if (checkInitStateAndContext() && device != null) {
            connectionMap[device.addr]?.refresh()
        }
    }

    /**
     * 根据MAC地址获取设备的配对状态
     *
     * @return [BluetoothDevice.BOND_NONE], [BluetoothDevice.BOND_BONDING], [BluetoothDevice.BOND_BONDED].
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
     * 开始配对
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
     * 根据过滤器，清除配对
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
     * 解除配对
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
            instance.getObservable().notifyLogChanged(msg, BleLogger.getLevel(priority))
            instance.logger.println("blelib:" + cls.simpleName, priority, msg)
        }
    }
}
