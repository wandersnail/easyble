package com.snail.easyble.core

import android.Manifest
import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.Log
import com.snail.easyble.callback.ScanListener
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*



/**
 * 描述: 搜索设备
 * 时间: 2018/12/19 20:11
 * 作者: zengfansheng
 */
internal class Scanner(private val bluetoothAdapter: BluetoothAdapter, private val mainThreadHandler: Handler, private val config: BleConfig) {
    /**
     * 是否正在扫描
     */
    var isScanning: Boolean = false
        private set
    private var bleScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var leScanCallback: BluetoothAdapter.LeScanCallback? = null
    private val scanListeners = ArrayList<ScanListener>()
    private var resultCallback: (BluetoothDevice, Int, ByteArray?) -> Unit = { _, _, _ -> }
    
    //判断位置服务是否打开
    private fun isLocationEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            locationManager?.isLocationEnabled ?: false
        } else {
            try {
                val locationMode = Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
                locationMode != Settings.Secure.LOCATION_MODE_OFF
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
                false
            }
        }
    }
    
    /**
     * 添加扫描监听器
     */
    @Synchronized
    fun addScanListener(listener: ScanListener?) {
        if (listener != null && !scanListeners.contains(listener)) {
            scanListeners.add(listener)
        }
    }

    /**
     * 移除扫描监听器
     */
    @Synchronized
    fun removeScanListener(listener: ScanListener) {
        scanListeners.remove(listener)
    }

    //是否缺少定位权限
    private fun noLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
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
            val method = bluetoothAdapter.javaClass.getDeclaredMethod("getConnectionState")
            //打开权限  
            method.isAccessible = true
            val state = method.invoke(bluetoothAdapter) as Int
            if (state == BluetoothAdapter.STATE_CONNECTED) {
                val devices = bluetoothAdapter.bondedDevices
                for (device in devices) {
                    val isConnectedMethod = device.javaClass.getDeclaredMethod("isConnected")
                    isConnectedMethod.isAccessible = true
                    val isConnected = isConnectedMethod.invoke(device) as Boolean
                    if (isConnected) {
                        resultCallback(device, 0, null)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val stopScanRunnable = Runnable { stopScan() }
    
    /**
     * 搜索蓝牙设备
     */
    fun startScan(context: Context, resultCallback: (BluetoothDevice, Int, ByteArray?) -> Unit) {
        this.resultCallback = resultCallback
        synchronized(this) {
            if (!bluetoothAdapter.isEnabled || isScanning) {
                return
            }
            if (!isLocationEnabled(context)) {
                handleScanCallback(false, null, ScanListener.ERROR_LOCATION_SERVICE_CLOSED, "位置服务未开启，无法搜索蓝牙设备")
                return
            } else if (noLocationPermission(context)) {
                handleScanCallback(false, null, ScanListener.ERROR_LACK_LOCATION_PERMISSION, "缺少定位权限，无法搜索蓝牙设备")
                return
            }
            isScanning = true
        }
        handleScanCallback(true, null, -1, "")
        if (config.isAcceptSysConnectedDevice) {
            getSystemConnectedDevices()
        }
        //如果是高版本使用新的搜索方法
        if (config.isUseBluetoothLeScanner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (bleScanner == null) {
                bleScanner = bluetoothAdapter.bluetoothLeScanner
            }
            if (scanCallback == null) {
                scanCallback = MyScanCallback()
            }
            if (config.scanSettings == null) {
                bleScanner!!.startScan(scanCallback)
            } else {
                bleScanner!!.startScan(null, config.scanSettings, scanCallback)
            }
        } else {
            if (leScanCallback == null) {
                leScanCallback = MyLeScanCallback()
            }
            bluetoothAdapter.startLeScan(leScanCallback)
        }
        mainThreadHandler.postDelayed(stopScanRunnable, config.scanPeriodMillis.toLong())
    }
    
    /**
     * 停止搜索蓝牙设备
     */
    fun stopScan() {
        mainThreadHandler.removeCallbacks(stopScanRunnable)
        if (!bluetoothAdapter.isEnabled) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (bleScanner != null && scanCallback != null) {
                bleScanner!!.stopScan(scanCallback)
            }
        }
        if (leScanCallback != null) {
            bluetoothAdapter.stopLeScan(leScanCallback)
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
            parseScanResult(result.device, result.rssi, scanRecord?.bytes, result)
        }
    }

    private inner class MyLeScanCallback : BluetoothAdapter.LeScanCallback {
        override fun onLeScan(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray) {
            parseScanResult(device, rssi, scanRecord, null)
        }
    }

    /**
     * 解析广播字段
     *
     * @param device     蓝牙设备
     * @param rssi       信号强度
     * @param advData 广播内容
     */
    fun parseScanResult(device: BluetoothDevice, rssi: Int, advData: ByteArray?, detailResult: ScanResult?) {
        if (config.isHideNonBleDevice && device.type != BluetoothDevice.DEVICE_TYPE_LE) {
            return
        }
        resultCallback(device, rssi, advData)
        val deviceName = if (TextUtils.isEmpty(device.name)) "Unknown Device" else device.name
        //生成
        var dev: Device? = null
        if (config.scanHandler != null) {
            val scanHandler = config.scanHandler!!
            //三个为空则不过滤
            if ((scanHandler.names.isEmpty() && scanHandler.addrs.isEmpty() && scanHandler.uuids.isEmpty()) ||
                    (scanHandler.names.contains(device.name) || scanHandler.addrs.contains(device.address) || acceptUuid(scanHandler.uuids, advData))){
                //只在指定的过滤器通知
                dev = scanHandler.handleAdvertisingData(device, advData!!)
            }         
        }
        if (dev != null || config.scanHandler == null) {
            if (dev == null) {
                dev = Device()
            }
            dev.name = if (TextUtils.isEmpty(dev.name)) deviceName else dev.name
            dev.addr = device.address
            dev.rssi = rssi
            dev.bondState = device.bondState
            dev.originalDevice = device
            dev.scanRecord = advData
            if (detailResult != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    dev.isConnectable = detailResult.isConnectable
                }
            }
            handleScanCallback(false, dev, -1, "")
        }
        Ble.println(Ble::class.java, Log.DEBUG, "found device! [name: $deviceName, mac: ${device.address}]")
    }

    private fun acceptUuid(uuids: List<UUID>, advData: ByteArray?): Boolean {
        try {
            val buffer = ByteBuffer.wrap(advData).order(ByteOrder.LITTLE_ENDIAN)
            while (buffer.remaining() > 2) {
                var length = buffer.get().toInt()
                if (length == 0) break

                val type = buffer.get().toInt()
                when (type) {
                    0x02, // Partial list of 16-bit UUIDs
                    0x03 // Complete list of 16-bit UUIDs
                    -> while (length >= 2) {
                        if (uuids.contains(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", buffer.short)))) {
                            return true
                        }
                        length -= 2
                    }
                    0x06, // Partial list of 128-bit UUIDs
                    0x07 // Complete list of 128-bit UUIDs
                    -> while (length >= 16) {
                        val lsb = buffer.long
                        val msb = buffer.long
                        if (uuids.contains(UUID(msb, lsb))) {
                            return true
                        }
                        length -= 16
                    }
                    else -> buffer.position(buffer.position() + length - 1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
        
    fun onBluethoothOff() {
        isScanning = false
        handleScanCallback(false, null, -1, "")
    }
    
    fun release() {
        scanListeners.clear()
        stopScan()
    }
}