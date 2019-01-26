package cn.zfs.bledemo

import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.snail.commons.utils.ToastUtils
import com.snail.easyble.annotation.InvokeThread
import com.snail.easyble.annotation.RunOn
import com.snail.easyble.callback.*
import com.snail.easyble.core.*
import com.snail.easyble.util.BleUtils
import kotlinx.android.synthetic.main.activity_connection.*
import java.util.*

/**
 * 
 * date: 2018/6/16 14:01
 * author: zengfansheng
 */
class ConnectionActivity : AppCompatActivity() {
    private var device: Device? = null
    private val UUID_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val UUID_RX_CHAR = UUID.fromString("0000ffe3-0000-1000-8000-00805f9b34fb")
    private val UUID_NOTIFICATION_CHAR = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")


    private val UUID_SERVICE_1 = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
    private val UUID_READ = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "功能列表"
        setContentView(R.layout.activity_connection)
        device = intent.getParcelableExtra(Consts.EXTRA_DEVICE)
        Ble.instance.registerObserver(eventObserver)        
        Ble.instance.connect(device!!, getConnectionConfig(true), null)        
        tvName.text = device!!.name
        tvAddr.text = device!!.addr
    }

    private fun getConnectionConfig(autoReconnect: Boolean): ConnectionConfig {
        val config = ConnectionConfig()
        config.setDiscoverServicesDelayMillis(500)
        config.setAutoReconnect(autoReconnect)
        return config
    }     

    private val eventObserver = object : SimpleEventObserver() {
        @InvokeThread(RunOn.MAIN)
        override fun onConnectionStateChanged(device: Device) {
            when (device.connectionState) {
                IConnection.STATE_CONNECTED -> {
                    tvState.text = "连接成功，等待发现服务"
                }
                IConnection.STATE_CONNECTING -> {
                    tvState.text = "连接中..."
                }
                IConnection.STATE_DISCONNECTED -> {
                    tvState.text = "连接断开"
                    ToastUtils.showShort("连接断开")
                }
                IConnection.STATE_SCANNING -> {
                    tvState.text = "正在搜索设备..."
                }
                IConnection.STATE_SERVICE_DISCOVERING -> {
                    tvState.text = "连接成功，正在发现服务..."
                }
                IConnection.STATE_SERVICE_DISCOVERED -> {
                    tvState.text = "连接成功，并成功发现服务"
                    val connection = Ble.instance.getConnection(device)
                    connection?.setCharacteristicChangedCallback(object : CharacteristicChangedCallback {
                        @InvokeThread(RunOn.POSTING)
                        override fun onCharacteristicChanged(device: Device, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {
                            Ble.println(javaClass, Log.ERROR, "数据：${BleUtils.bytesToHexString(value)}, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                        }
                    })
                    connection?.enableNotification("", UUID_SERVICE, UUID_NOTIFICATION_CHAR, object : NotificationChangedCallback {
                        @InvokeThread(RunOn.MAIN)
                        override fun onNotificationChanged(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, isEnabled: Boolean) {
                            Ble.println(javaClass, Log.ERROR, "RequestCallback toggleNotification onSuccess, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                        }

                        override fun onRequestFailed(device: Device, tag: String, requestType: Request.RequestType, failType: Int, src: ByteArray?) {
                            Ble.println(javaClass, Log.ERROR, "RequestCallback toggleNotification onFail, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                        }
                    })
                    connection?.writeCharacteristic("", UUID_SERVICE, UUID_RX_CHAR, byteArrayOf(0xa5.toByte()), object : CharacteristicWriteCallback {
                        @InvokeThread(RunOn.BACKGROUND)
                        override fun onCharacteristicWrite(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {
                            Ble.println(javaClass, Log.ERROR, "RequestCallback writeCharacteristic onSuccess, 数据：${BleUtils.bytesToHexString(value)}, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                        }

                        override fun onRequestFailed(device: Device, tag: String, requestType: Request.RequestType, failType: Int, src: ByteArray?) {
                            Ble.println(javaClass, Log.ERROR, "RequestCallback writeCharacteristic onFail, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                        }
                    })

                    connection?.readRssi("", object : RemoteRssiReadCallback {
                        override fun onRemoteRssiRead(device: Device, tag: String, rssi: Int) {
                            Ble.println(javaClass, Log.ERROR, "RequestCallback readRssi onSuccess, rssi: $rssi")
                        }

                        override fun onRequestFailed(device: Device, tag: String, requestType: Request.RequestType, failType: Int, src: ByteArray?) {
                            Ble.println(javaClass, Log.ERROR, "RequestCallback readRssi onFail, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                        }
                    })

                    connection?.readCharacteristic("", UUID_SERVICE_1, UUID_READ, object : CharacteristicReadCallback {
                        override fun onCharacteristicRead(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {
                            Ble.println(javaClass, Log.ERROR, "RequestCallback readCharacteristic onSuccess, Device Name: ${String(value)}, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                        }

                        override fun onRequestFailed(device: Device, tag: String, requestType: Request.RequestType, failType: Int, src: ByteArray?) {
                            Ble.println(javaClass, Log.DEBUG, "RequestCallback readCharacteristic onFail, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                        }
                    })

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        connection?.changeMtu("", 256, object : MtuChangedCallback {
                            @InvokeThread(RunOn.MAIN)
                            override fun onMtuChanged(device: Device, tag: String, mtu: Int) {
                                Ble.println(javaClass, Log.ERROR, "RequestCallback changeMtu onSuccess, mtu: $mtu, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                            }

                            @InvokeThread(RunOn.MAIN)
                            override fun onRequestFailed(device: Device, tag: String, requestType: Request.RequestType, failType: Int, src: ByteArray?) {
                                Ble.println(javaClass, Log.ERROR, "RequestCallback changeMtu onFail, uiThread: ${Looper.getMainLooper() == Looper.myLooper()}")
                            }
                        })
                    }
                }
                IConnection.STATE_RELEASED -> {
                    tvState.text = "连接已释放"
                }
            }
            invalidateOptionsMenu()
        }

        override fun onConnectFailed(device: Device?, type: Int) {
            tvState.text = "连接失败： $type"
        }

        override fun onConnectTimeout(device: Device, type: Int) {
            val msg = when(type) {
                IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE -> "无法搜索到设备"
                IConnection.TIMEOUT_TYPE_CANNOT_CONNECT -> "无法连接设备"
                else -> "无法发现蓝牙服务"
            }
            ToastUtils.showShort("连接超时:$msg")
        }
    }
        
    override fun onDestroy() {
        super.onDestroy()
        Ble.instance.unregisterObserver(eventObserver)
        Ble.instance.releaseConnection(device)//销毁连接
    }
}