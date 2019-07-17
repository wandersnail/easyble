package com.snail.easyble.core

import android.Manifest
import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import androidx.core.content.ContextCompat
import com.snail.easyble.callback.ScanListener
import com.snail.easyble.util.BleLogger
import com.snail.easyble.util.BleUtils


/**
 * 蓝牙搜索器，处理蓝牙搜索过程的事件，[Device]的实例化等
 * 
 * date: 2018/12/19 20:11
 * author: zengfansheng
 */
internal class Scanner(private val bluetoothAdapter: BluetoothAdapter, private val mainThreadHandler: Handler) {
    private var isScanning = false
    private var bleScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var leScanCallback: BluetoothAdapter.LeScanCallback? = null
    private val scanListeners = ArrayList<ScanListener>()
    private var internalScanListener: InternalScanListener? = null
    private var proxyBluetoothProfiles = SparseArray<BluetoothProfile>()

    //位置服务是否开户
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
        
    private fun getScanConfig(): ScanConfig {
        return Ble.instance.bleConfig.scanConfig
    }

    @Synchronized
    fun addScanListener(listener: ScanListener?) {
        if (listener != null && !scanListeners.contains(listener)) {
            scanListeners.add(listener)
        }
    }

    @Synchronized
    fun removeScanListener(listener: ScanListener) {
        scanListeners.remove(listener)
    }

    //检查是否有定位权限
    private fun noLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
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

    private fun getSystemConnectedDevices(context: Context, profile: Int) {
        bluetoothAdapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceDisconnected(profile: Int) {}

            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (proxy != null) {
                    proxyBluetoothProfiles.put(profile, proxy)
                }
                synchronized(this@Scanner) {
                    if (!isScanning) {
                        return
                    }
                }
                try {
                    proxy?.connectedDevices?.forEach {
                        parseScanResult(it, 0, null, null)
                    }
                } catch (e: Exception) {}
            }
        }, profile)
    }

    private fun getSystemConnectedDevices(context: Context) {
        try {
            val method = bluetoothAdapter.javaClass.getDeclaredMethod("getConnectionState")
            method.isAccessible = true
            val state = method.invoke(bluetoothAdapter) as Int
            if (state == BluetoothAdapter.STATE_CONNECTED) {
                val devices = bluetoothAdapter.bondedDevices
                for (device in devices) {
                    val isConnectedMethod = device.javaClass.getDeclaredMethod("isConnected")
                    isConnectedMethod.isAccessible = true
                    val isConnected = isConnectedMethod.invoke(device) as Boolean
                    if (isConnected) {
                        parseScanResult(device, 0, null, null)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        //遍历支持的，获取所有连接的
        for (i in 1..21) {
            try {
                getSystemConnectedDevices(context, i)
            } catch (e: Exception) {}
        }
    }

    private val stopScanRunnable = Runnable { stopScan() }
    
    /**
     * 开始搜索
     */
    fun startScan(context: Context, callback: InternalScanListener?) {
        this.internalScanListener = callback
        synchronized(this) {
            if (!bluetoothAdapter.isEnabled || isScanning) {
                return
            }
            if (!isLocationEnabled(context)) {
                val errorMsg = "Unable to scan for Bluetooth devices, the phone's location service is not turned on."
                handleScanCallback(false, null, ScanListener.ERROR_LOCATION_SERVICE_CLOSED, errorMsg)
                Ble.instance.logger.handleLog(Log.ERROR, errorMsg, BleLogger.TYPE_SCAN_STATE)
                return
            } else if (noLocationPermission(context)) {
                val errorMsg = "Unable to scan for Bluetooth devices, lack location permission."
                handleScanCallback(false, null, ScanListener.ERROR_LACK_LOCATION_PERMISSION, errorMsg)
                Ble.instance.logger.handleLog(Log.ERROR, errorMsg, BleLogger.TYPE_SCAN_STATE)
                return
            }
            isScanning = true
        }
        handleScanCallback(true, null, -1, "")
        if (getScanConfig().isAcceptSysConnectedDevice) {
            getSystemConnectedDevices(context)
        }
        if (getScanConfig().isUseBluetoothLeScanner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (bleScanner == null) {
                bleScanner = bluetoothAdapter.bluetoothLeScanner
            }
            if (scanCallback == null) {
                scanCallback = MyScanCallback()
            }
            if (getScanConfig().scanSettings == null) {
                bleScanner!!.startScan(scanCallback)
            } else {
                bleScanner!!.startScan(null, getScanConfig().scanSettings, scanCallback)
            }
        } else {
            if (leScanCallback == null) {
                leScanCallback = MyLeScanCallback()
            }
            bluetoothAdapter.startLeScan(leScanCallback)
        }
        mainThreadHandler.postDelayed(stopScanRunnable, getScanConfig().scanPeriodMillis.toLong())
    }
    
    /**
     * 停止搜索
     */
    @JvmOverloads
    fun stopScan(quietly: Boolean = false) {
        mainThreadHandler.removeCallbacks(stopScanRunnable)
        val size = proxyBluetoothProfiles.size()
        for (i in 0 until size) {
            try {
                bluetoothAdapter.closeProfileProxy(proxyBluetoothProfiles.keyAt(i), proxyBluetoothProfiles.valueAt(i))
            } catch (e: Exception) {}
        }
        proxyBluetoothProfiles.clear()
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
            internalScanListener?.onScanStop()
            if (!quietly) {
                handleScanCallback(false, null, -1, "")
            }
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
     * 解析广播数据
     *
     * @param advData 原始广播数据
     */
    fun parseScanResult(device: BluetoothDevice, rssi: Int, advData: ByteArray?, detailResult: ScanResult?) {
        if ((getScanConfig().isOnlyAcceptBleDevice && device.type != BluetoothDevice.DEVICE_TYPE_LE) ||
            !device.address.matches("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$".toRegex())) {
            return
        }
        internalScanListener?.onScanResult(device, rssi, advData)
        val origDevName = device.name
        val deviceName = if (TextUtils.isEmpty(origDevName)) "" else origDevName
        if ((getScanConfig().names.isEmpty() && getScanConfig().addrs.isEmpty() && getScanConfig().uuids.isEmpty() && getScanConfig().rssiLimit <= rssi) ||
                getScanConfig().names.contains(origDevName) || getScanConfig().addrs.contains(device.address) || BleUtils.hasUuid(getScanConfig().uuids, advData)){
            //通过构建器实例化Device
            val deviceCreater = Ble.instance.bleConfig.deviceCreator
            var dev = deviceCreater?.valueOf(device, advData)
            if (dev != null || deviceCreater == null) {
                if (dev == null) {
                    dev = Device(device)
                }
                dev.name = if (TextUtils.isEmpty(dev.name)) deviceName else dev.name
                dev.rssi = rssi
                dev.bondState = device.bondState
                dev.advData = advData
                if (detailResult != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        dev.isConnectable = detailResult.isConnectable
                    }
                }
                handleScanCallback(false, dev, -1, "")
            }
        }
        Ble.instance.logger.handleLog(Log.DEBUG, "found device! [name: ${if (deviceName.isEmpty()) "N/A" else deviceName}, addr: ${device.address}]", BleLogger.TYPE_SCAN_STATE)
    }
        
    fun onBluethoothOff() {
        isScanning = false
        handleScanCallback(false, null, -1, "")
    }
    
    fun release() {
        scanListeners.clear()
        stopScan()
    }

    internal interface InternalScanListener {
        /**
         * 蓝牙搜索停止
         */
        fun onScanStop()

        /**
         * 搜索到BLE设备
         */
        fun onScanResult(device: BluetoothDevice, rssi: Int, advData: ByteArray?)
    }
}