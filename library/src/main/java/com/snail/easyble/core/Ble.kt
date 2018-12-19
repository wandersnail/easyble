package com.snail.easyble.core

import android.Manifest
import android.annotation.TargetApi
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.Log
import com.snail.easyble.callback.ConnectionStateChangeListener
import com.snail.easyble.callback.ScanListener
import com.snail.easyble.event.Events
import org.greenrobot.eventbus.EventBus
import java.util.*
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
     * 是否正在扫描
     */
    var isScanning: Boolean = false
        private set
    private var bleScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var leScanCallback: BluetoothAdapter.LeScanCallback? = null
    /**
     * 替换默认配置
     */
    var bleConfig: BleConfig = BleConfig()
    private val scanListeners: MutableList<ScanListener>
    private val mainThreadHandler: Handler
    private val publisher: EventBus
    private val logger: BleLogger
    private val executorService: ExecutorService
    private var app: Application? = null

    init {
        connectionMap = ConcurrentHashMap()
        mainThreadHandler = Handler(Looper.getMainLooper())
        scanListeners = ArrayList()
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
                        isScanning = false
                        handleScanCallback(false, null, -1, "")
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

    //判断位置服务是否打开
    private val isLocationEnabled: Boolean
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val locationManager = app!!.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                locationManager?.isLocationEnabled ?: false
            } else {
                try {
                    val locationMode = Settings.Secure.getInt(app!!.contentResolver, Settings.Secure.LOCATION_MODE)
                    locationMode != Settings.Secure.LOCATION_MODE_OFF
                } catch (e: Settings.SettingNotFoundException) {
                    e.printStackTrace()
                    false
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

    private val stopScanRunnable = Runnable { stopScan() }

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
        return true
    }

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
        return isInited
    }

    /**
     * 关闭所有连接，释放资源
     */
    fun release() {
        if (checkInitStateAndContext()) {
            stopScan()
            scanListeners.clear()
            releaseAllConnections() //释放所有连接
        }
    }

    //是否缺少定位权限
    private fun noLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(app!!, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED || 
                ContextCompat.checkSelfPermission(app!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
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
        if (listener != null && !scanListeners.contains(listener)) {
            scanListeners.add(listener)
        }
    }

    /**
     * 移除扫描监听器
     */
    fun removeScanListener(listener: ScanListener) {
        scanListeners.remove(listener)
    }

    /**
     * 搜索蓝牙设备
     */
    fun startScan() {
        synchronized(this) {
            if (!checkInitStateAndContext() || bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled || isScanning) {
                return
            }
            if (!isLocationEnabled) {
                handleScanCallback(false, null, ScanListener.ERROR_LOCATION_SERVICE_CLOSED, "位置服务未开启，无法搜索蓝牙设备")
                return
            } else if (noLocationPermission()) {
                handleScanCallback(false, null, ScanListener.ERROR_LACK_LOCATION_PERMISSION, "缺少定位权限，无法搜索蓝牙设备")
                return
            }
            isScanning = true
        }
        handleScanCallback(true, null, -1, "")
        if (bleConfig.isAcceptSysConnectedDevice) {
            getSystemConnectedDevices()
        }
        //如果是高版本使用新的搜索方法
        if (bleConfig.isUseBluetoothLeScanner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (bleScanner == null) {
                bleScanner = bluetoothAdapter!!.bluetoothLeScanner
            }
            if (scanCallback == null) {
                scanCallback = MyScanCallback()
            }
            if (bleConfig.scanSettings == null) {
                bleScanner!!.startScan(scanCallback)
            } else {
                bleScanner!!.startScan(null, bleConfig.scanSettings, scanCallback)
            }
        } else {
            if (leScanCallback == null) {
                leScanCallback = MyLeScanCallback()
            }
            bluetoothAdapter!!.startLeScan(leScanCallback)
        }
        mainThreadHandler.postDelayed(stopScanRunnable, bleConfig.scanPeriodMillis.toLong())
    }

    private fun handleScanCallback(start: Boolean, device: Device?, errorCode: Int, errorMsg: String) {
        mainThreadHandler.post {
            for (listener in scanListeners) {
                when {
                    device != null -> listener.onScanResult(device)
                    start -> listener.onScanStart()
                    errorCode >= 0 -> listener.onScanError(errorCode, errorMsg)
                    else -> listener.onScanStop()
                }
            }
        }
    }

    //获取系统已连接的设备
    private fun getSystemConnectedDevices() {
        try {
            //得到连接状态的方法
            val method = bluetoothAdapter!!.javaClass.getDeclaredMethod("getConnectionState")
            //打开权限  
            method.isAccessible = true
            val state = method.invoke(bluetoothAdapter) as Int
            if (state == BluetoothAdapter.STATE_CONNECTED) {
                val devices = bluetoothAdapter!!.bondedDevices
                for (device in devices) {
                    val isConnectedMethod = device.javaClass.getDeclaredMethod("isConnected")
                    isConnectedMethod.isAccessible = true
                    val isConnected = isConnectedMethod.invoke(device) as Boolean
                    if (isConnected) {
                        parseScanResult(device, 0, null)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 停止搜索蓝牙设备
     */
    fun stopScan() {
        if (!checkInitStateAndContext()) {
            return
        }
        mainThreadHandler.removeCallbacks(stopScanRunnable)
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (bleScanner != null && scanCallback != null) {
                bleScanner!!.stopScan(scanCallback)
            }
        }
        if (leScanCallback != null) {
            bluetoothAdapter!!.stopLeScan(leScanCallback)
        }
        if (isScanning) {
            isScanning = false
            handleScanCallback(false, null, -1, "")
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private inner class MyScanCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val scanRecord = result.scanRecord
            parseScanResult(result.device, result.rssi, scanRecord?.bytes)
        }
    }

    private inner class MyLeScanCallback : BluetoothAdapter.LeScanCallback {
        override fun onLeScan(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray) {
            parseScanResult(device, rssi, scanRecord)
        }
    }

    /**
     * 解析广播字段
     *
     * @param device     蓝牙设备
     * @param rssi       信号强度
     * @param scanRecord 广播内容
     */
    fun parseScanResult(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?) {
        if (bleConfig.isHideNonBleDevice && device.type != BluetoothDevice.DEVICE_TYPE_LE) {
            return
        }
        for (connection in connectionMap.values) {
            connection.onScanResult(device.address)
        }
        val deviceName = if (TextUtils.isEmpty(device.name)) "Unknown Device" else device.name
        //生成
        var dev: Device? = null
        if (bleConfig.scanHandler != null) {
            //只在指定的过滤器通知
            dev = bleConfig.scanHandler!!.handle(device, scanRecord!!)
        }
        if (dev != null || bleConfig.scanHandler == null) {
            if (dev == null) {
                dev = Device()
            }
            dev.name = if (TextUtils.isEmpty(dev.name)) deviceName else dev.name
            dev.addr = device.address
            dev.rssi = rssi
            dev.bondState = device.bondState
            dev.originalDevice = device
            dev.scanRecord = scanRecord
            handleScanCallback(false, dev, -1, "")
        }
        println(Ble::class.java, Log.DEBUG, "found device! [name: $deviceName, mac: ${device.address}]")
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
