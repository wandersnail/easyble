package cn.zfs.bledemo

import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.snail.easyble.callback.CharacteristicChangedCallback
import com.snail.easyble.callback.RequestCallback
import com.snail.easyble.core.*
import com.snail.easyble.event.Events
import com.snail.easyble.util.BleUtils
import com.zfs.commons.utils.ToastUtils
import kotlinx.android.synthetic.main.activity_connection.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

/**
 * 描述:
 * 时间: 2018/6/16 14:01
 * 作者: zengfansheng
 */
class ConnectionActivity : AppCompatActivity() {
    private var device: Device? = null
    private val UUID_SERVICE = UUID.fromString("0000ffff-0000-1000-8000-00805f9b34fb")
    private val UUID_RX_CHAR = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")
    private val UUID_NOTIFICATION_CHAR = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")


    private val UUID_SERVICE_1 = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
    private val UUID_READ = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "功能列表"
        setContentView(R.layout.activity_connection)
        device = intent.getParcelableExtra(Consts.EXTRA_DEVICE)
        Ble.getInstance().registerSubscriber(this)        
        Ble.getInstance().connect(device!!, getConnectionConfig(true), null)        
        tvName.text = device!!.name
        tvAddr.text = device!!.addr
    }

    private fun getConnectionConfig(autoReconnect: Boolean): ConnectionConfig {
        val config = ConnectionConfig.newInstance()
        config.setDiscoverServicesDelayMillis(500)
        config.isAutoReconnect = autoReconnect
        return config
    }     

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConnectionStateChange(e: Events.ConnectionStateChanged) {
        when (e.state) {
            Connection.STATE_CONNECTED -> {
                tvState.text = "连接成功，等待发现服务"
            }
            Connection.STATE_CONNECTING -> {
                tvState.text = "连接中..."
            }
            Connection.STATE_DISCONNECTED -> {
                tvState.text = "连接断开"
                ToastUtils.showShort("连接断开")
            }
            Connection.STATE_SCANNING -> {
                tvState.text = "正在搜索设备..."
            }
            Connection.STATE_SERVICE_DISCOVERING -> {
                tvState.text = "连接成功，正在发现服务..."
            }
            Connection.STATE_SERVICE_DISCOVERED -> {
                tvState.text = "连接成功，并成功发现服务"
                val connection = Ble.getInstance().getConnection(e.device)
                connection.setCharacteristicChangedCallback(object : CharacteristicChangedCallback {
                    @CallOnUiThread
                    override fun onCharacteristicChanged(characteristic: BluetoothGattCharacteristic) {
                        Ble.println(javaClass, Log.ERROR, "数据：${BleUtils.bytesToHexString(characteristic.value)}, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                    }
                })
                connection.toggleNotification("", UUID_SERVICE, UUID_NOTIFICATION_CHAR, true, object : RequestCallback<Events.NotificationChanged> {
                    @CallOnBackgroundThread
                    override fun onSuccess(data: Events.NotificationChanged) {
                        Ble.println(javaClass, Log.ERROR, "RequestCallback onSuccess, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                    }

                    override fun onFail(requestFailed: Events.RequestFailed) {
                        Ble.println(javaClass, Log.ERROR, "RequestCallback onFail, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                    }
                })
                connection.writeCharacteristic("", UUID_SERVICE, UUID_RX_CHAR, byteArrayOf(0xa5.toByte()), object : RequestCallback<Events.CharacteristicWrite> {
                    @CallOnUiThread
                    override fun onSuccess(data: Events.CharacteristicWrite) {
                        Ble.println(javaClass, Log.ERROR, "RequestCallback onSuccess, 数据：${BleUtils.bytesToHexString(data.characteristic.value)}, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                    }

                    override fun onFail(requestFailed: Events.RequestFailed) {
                        Ble.println(javaClass, Log.ERROR, "RequestCallback onFail, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                    }
                })
                
                connection.readRssi("", object : RequestCallback<Events.RemoteRssiRead> {

                    override fun onSuccess(data: Events.RemoteRssiRead) {
                        Ble.println(javaClass, Log.ERROR, "RequestCallback onSuccess, rssi: ${data.rssi}")
                    }

                    override fun onFail(requestFailed: Events.RequestFailed) {
                        Ble.println(javaClass, Log.ERROR, "RequestCallback onFail, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                    }
                })
                
                connection.readCharacteristic("", UUID_SERVICE_1, UUID_READ, object : RequestCallback<Events.CharacteristicRead> {
                    override fun onSuccess(data: Events.CharacteristicRead) {
                        Ble.println(javaClass, Log.ERROR, "RequestCallback onSuccess, Device Name: ${String(data.characteristic.value)}, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                    }

                    override fun onFail(requestFailed: Events.RequestFailed) {
                        Ble.println(javaClass, Log.DEBUG, "RequestCallback onFail, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                    }
                })
            }
            Connection.STATE_RELEASED -> {
                tvState.text = "连接已释放"
            }
        }
        invalidateOptionsMenu()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConnectFailed(e: Events.ConnectFailed) {
        tvState.text = "连接失败： ${e.type}"
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConnectTimeout(e: Events.ConnectTimeout) {
        val msg = when(e.type) {
            Connection.TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE -> "无法搜索到设备"
            Connection.TIMEOUT_TYPE_CANNOT_CONNECT -> "无法连接设备"
            else -> "无法发现蓝牙服务"
        }
        ToastUtils.showShort("连接超时:$msg")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Ble.getInstance().unregisterSubscriber(this)
        Ble.getInstance().releaseConnection(device)//销毁连接
    }
}