package cn.zfs.bledemo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.snail.commons.helper.PermissionsRequester
import com.snail.commons.utils.PreferencesUtils
import com.snail.easyble.callback.ScanListener
import com.snail.easyble.core.Ble
import com.snail.easyble.core.Device
import com.snail.easyble.core.ScanConfig
import com.snail.widget.listview.BaseListAdapter
import com.snail.widget.listview.BaseViewHolder
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private var permissionsRequester: PermissionsRequester? = null
    private var listAdapter: ListAdapter? = null
    private val devList = ArrayList<Device>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialize()
        initViews()             
        Ble.instance.addScanListener(scanListener)
        val scanConfig = ScanConfig().setAcceptSysConnectedDevice(false)
                .setHideNonBleDevice(true)
                .setUseBluetoothLeScanner(PreferencesUtils.getBoolean(Consts.SP_KEY_USE_NEW_SCANNER, true))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanConfig.setScanSettings(ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build())
        }
        Ble.instance.bleConfig.setScanConfig(scanConfig)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menuStop -> {
                if (Ble.instance.isInitialized) {
                    refreshLayout.isRefreshing = false
                    Ble.instance.stopScan()
                }
            }
        }
        invalidateOptionsMenu()
        return true
    }

    private fun initialize() {
        //动态申请权限
        permissionsRequester = PermissionsRequester(this)
        permissionsRequester?.setOnRequestResultListener(object : PermissionsRequester.OnRequestResultListener {
            override fun onRequestResult(refusedPermissions: MutableList<String>) {
                
            }
        })
        permissionsRequester?.checkAndRequest(getNeedPermissions())
    }

    //需要进行检测的权限
    private fun getNeedPermissions(): MutableList<String> {
        val list = ArrayList<String>()
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
        Log.d("Main", "onResume")
        if (Ble.instance.isInitialized) {
            if (Ble.instance.isBluetoothAdapterEnabled) {
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
        
        override fun createViewHolder(position: Int): BaseViewHolder<Device> {
            return object : BaseViewHolder<Device>() {
                var tvName: TextView? = null
                var tvAddr: TextView? = null
                var tvRssi: TextView? = null
                
                override fun createView(): View {
                    val view = View.inflate(context, R.layout.item_scan, null)
                    tvName = view.findViewById(R.id.tvName)
                    tvAddr = view.findViewById(R.id.tvAddr)
                    tvRssi = view.findViewById(R.id.tvRssi)
                    return view
                }

                override fun onBind(item: Device, position: Int) {
                    rssiViews[item.addr] = tvRssi!!
                    tvName?.text = item.name
                    tvAddr?.text = item.addr
                    tvRssi?.text = item.rssi.toString()
                }
            }
        }

        fun clear() {
            items.clear()
            notifyDataSetChanged()
        }
        
        fun add(device: Device) {
            val dev = items.firstOrNull { it.addr == device.addr }
            if (dev == null) {
                updateTimeMap[device.addr] = System.currentTimeMillis()
                items.add(device)
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
        Log.d("Main", "doStartScan")
    }

    private val scanListener = object : ScanListener {
        override fun onScanStart() {
            if (!refreshLayout.isRefreshing) {
                refreshLayout.isRefreshing = true
            }
        }

        override fun onScanStop() {
            refreshLayout.isRefreshing = false            
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
