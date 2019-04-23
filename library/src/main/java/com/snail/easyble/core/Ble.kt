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
import android.util.Log
import com.snail.easyble.callback.ConnectionStateChangeListener
import com.snail.easyble.callback.ScanListener
import com.snail.easyble.util.BleLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 
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
    private val executorService: ExecutorService
    private var app: Application? = null
    private var scanner: Scanner? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private val methodPoster: MethodPoster

    init {
        connectionMap = ConcurrentHashMap()
        mainHandler = Handler(Looper.getMainLooper())
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
            if (BluetoothAdapter.ACTION_STATE_CHANGED == intent.action) { //蓝牙开关状态变化               
                if (bluetoothAdapter != null) {
                    getObservable().notifyBluetoothStateChanged(bluetoothAdapter!!.state)
                    if (bluetoothAdapter!!.state == BluetoothAdapter.STATE_OFF) { //蓝牙关闭
                        scanner?.onBluethoothOff()
                        //断开所有连接
                        connectionMap.values.forEach { 
                            it.disconnect()
                        }
                    } else if (bluetoothAdapter!!.state == BluetoothAdapter.STATE_ON) {
                        connectionMap.values.forEach {
                            if (it.isAutoReconnectEnabled) {
                                it.reconnect() //重连所有设置了自动重连的连接
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Application不为空并且已初始化返回true
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

    //尝试通过反射获取Application实例
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
     * 使用SDK之前，必须先初始化
     */
    @Synchronized
    fun initialize(app: Application): Boolean {
        if (isInitialized) {
            return true
        }
        this.app = app
        //检查是否支持BLE
        if (!app.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false
        }
        //获取蓝牙配置器
        val bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (bluetoothManager == null || bluetoothManager.adapter == null) {
            return false
        }
        bluetoothAdapter = bluetoothManager.adapter
        //注册蓝牙开关状态广播接收者
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
                val message = "The SDK has not been initialized, make sure to call Ble.getInstance().initialize(Application) first."
                Exception(message).printStackTrace()
                BleLogger.handleLog(Log.ERROR, message)
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
     * 关闭所有连接并释放资源
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
     * 注册连接状态及数据接收观察者
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
     * 取消注册连接状态及数据接收观察者
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
     * 停止搜索，不触发回调
     */
    fun stopScanQuietly() {
        if (checkInitStateAndContext()) {
            scanner?.stopScan(true)
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
        //如果连接已存在，先释放掉
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
        if (device != null) {
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
        if (device != null) {
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
        val instance = Holder.BLE
    }
}
