package cn.zfs.bledemo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.snail.commons.base.BaseHolder
import com.snail.commons.base.BaseListAdapter
import com.snail.commons.entity.PermissionsRequester
import com.snail.commons.utils.PreferencesUtils
import com.snail.commons.utils.ToastUtils
import com.snail.easyble.callback.ScanListener
import com.snail.easyble.core.Ble
import com.snail.easyble.core.BleLogger
import com.snail.easyble.core.Device
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private var permissionsRequester: PermissionsRequester? = null
    private var scanning = false
    private var listAdapter: ListAdapter? = null
    private val devList = ArrayList<Device>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialize()
        initViews()     
        Ble.instance.setLogPrintLevel(BleLogger.ALL)//输出日志
        Ble.instance.addScanListener(scanListener)
        Ble.instance.bleConfig.setAcceptSysConnectedDevice(false)
        Ble.instance.bleConfig.setHideNonBleDevice(true)
        Ble.instance.bleConfig.setUseBluetoothLeScanner(PreferencesUtils.getBoolean(Consts.SP_KEY_USE_NEW_SCANNER, true))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Ble.instance.bleConfig.setScanSettings(ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        menu?.findItem(R.id.menuOld)?.isVisible = Ble.instance.bleConfig.isUseBluetoothLeScanner
        menu?.findItem(R.id.menuNew)?.isVisible = !Ble.instance.bleConfig.isUseBluetoothLeScanner
        ToastUtils.showShort("当前使用的是: ${if (Ble.instance.bleConfig.isUseBluetoothLeScanner) "新" else "旧"}扫描器")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menuOld -> {//使用旧扫描器
                Ble.instance.bleConfig.setUseBluetoothLeScanner(false)
                PreferencesUtils.putBoolean(Consts.SP_KEY_USE_NEW_SCANNER, false)
            }
            R.id.menuNew -> {//使用新扫描器
                Ble.instance.bleConfig.setUseBluetoothLeScanner(true)
                PreferencesUtils.putBoolean(Consts.SP_KEY_USE_NEW_SCANNER, true)
            }
        }
        invalidateOptionsMenu()
        return true
    }

    private fun initialize() {
        //动态申请权限
        permissionsRequester = PermissionsRequester(this)
        permissionsRequester?.setOnRequestResultListener { 
            if (it.isEmpty()) {
                
            }
        }
        permissionsRequester?.check(getNeedPermissions())
    }

    //需要进行检测的权限
    private fun getNeedPermissions(): List<String> {
        val list = java.util.ArrayList<String>()
        list.add(Manifest.permission.ACCESS_FINE_LOCATION)
        list.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        return list
    }
    
    private fun initViews() {
        refreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent))
        listAdapter = ListAdapter(this, devList)
        lv.adapter = listAdapter
        lv.setOnItemClickListener { _, _, position, _ ->
            val i = Intent(this, ConnectionActivity::class.java)
            i.putExtra(Consts.EXTRA_DEVICE, devList[position])
            startActivity(i)
        }
        refreshLayout.setOnRefreshListener {
            if (Ble.instance.isInitialized) {
                Ble.instance.stopScan()
                doStartScan()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Ble.instance.isInitialized) {
            if (Ble.instance.isBluetoothAdapterEnabled) {
                if (!refreshLayout.isRefreshing) {
                    refreshLayout.isRefreshing = true
                }
                doStartScan()
            } else {
                startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (Ble.instance.isInitialized) {
            Ble.instance.stopScan()
        }
    }
    
    private class ListAdapter(context: Context, data: MutableList<Device>) : BaseListAdapter<Device>(context, data) {
        private val updateTimeMap = HashMap<String, Long>()
        private val rssiViews = HashMap<String, TextView>()
        
        override fun getHolder(position: Int): BaseHolder<Device> {
            return object : BaseHolder<Device>() {
                var tvName: TextView? = null
                var tvAddr: TextView? = null
                var tvRssi: TextView? = null
                
                override fun createConvertView(): View {
                    val view = View.inflate(context, R.layout.item_scan, null)
                    tvName = view.findViewById(R.id.tvName)
                    tvAddr = view.findViewById(R.id.tvAddr)
                    tvRssi = view.findViewById(R.id.tvRssi)
                    return view
                }

                override fun setData(data: Device, position: Int) {
                    rssiViews[data.addr] = tvRssi!!
                    tvName?.text = data.name
                    tvAddr?.text = data.addr
                    tvRssi?.text = data.rssi.toString()
                }
            }
        }

        fun clear() {
            data.clear()
            notifyDataSetChanged()
        }
        
        fun add(device: Device) {
            val dev = data.firstOrNull { it.addr == device.addr }
            if (dev == null) {
                updateTimeMap[device.addr] = System.currentTimeMillis()
                data.add(device)
                notifyDataSetChanged()
            } else {
                val time = updateTimeMap[device.addr]
                if (time == null || System.currentTimeMillis() - time > 2000) {
                    updateTimeMap[device.addr] = System.currentTimeMillis()
                    if (dev.rssi != device.rssi) {
                        dev.rssi = device.rssi
                        val tvRssi = rssiViews[device.addr]
                        tvRssi?.text = device.rssi.toString()
                        tvRssi?.setTextColor(Color.BLACK)
                        tvRssi?.postDelayed({
                            tvRssi.setTextColor(0xFF909090.toInt())
                        }, 800)
                    }
                }
            }            
        }
    }
    
    private fun doStartScan() {
        listAdapter?.clear()
        layoutEmpty.visibility = View.VISIBLE
        Ble.instance.startScan()
    }

    private val scanListener = object : ScanListener {
        override fun onScanStart() {
            scanning = true            
        }

        override fun onScanStop() {
            refreshLayout.isRefreshing = false
            scanning = false            
        }

        override fun onScanResult(device: Device) {
            refreshLayout.isRefreshing = false
            layoutEmpty.visibility = View.INVISIBLE
            listAdapter?.add(device)
        }

        override fun onScanError(errorCode: Int, errorMsg: String) {
            
        }
    }
    
    override fun onDestroy() {    
        Ble.instance.release()
        super.onDestroy()
        exitProcess(0)
    }
}
