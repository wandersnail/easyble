package cn.zfs.bledemo;

import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import com.snail.commons.utils.StringUtilsKt;
import com.snail.commons.utils.ToastUtils;
import com.snail.easyble.annotation.InvokeThread;
import com.snail.easyble.annotation.RunOn;
import com.snail.easyble.callback.CharacteristicChangedCallback;
import com.snail.easyble.callback.CharacteristicReadCallback;
import com.snail.easyble.callback.CharacteristicWriteCallback;
import com.snail.easyble.callback.MtuChangedCallback;
import com.snail.easyble.callback.NotificationChangedCallback;
import com.snail.easyble.callback.RemoteRssiReadCallback;
import com.snail.easyble.core.Ble;
import com.snail.easyble.core.Connection;
import com.snail.easyble.core.ConnectionConfig;
import com.snail.easyble.core.Device;
import com.snail.easyble.core.EventObserver;
import com.snail.easyble.core.IConnection;
import com.snail.easyble.core.Request;
import com.snail.easyble.core.SimpleEventObserver;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * date: 2019/5/31 18:24
 * author: zengfansheng
 */
public class ConnectionActivity extends AppCompatActivity {
    private static final UUID UUID_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID UUID_RX_CHAR = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID UUID_NOTIFICATION_CHAR = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID UUID_SERVICE_1 = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_READ = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    
    private Device device;
    private TextView tvName;
    private TextView tvAddr;
    private TextView tvState;

    private void assignViews() {
        tvName = findViewById(R.id.tvName);
        tvAddr = findViewById(R.id.tvAddr);
        tvState = findViewById(R.id.tvState);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        assignViews();
        device = getIntent().getParcelableExtra(Consts.EXTRA_DEVICE);
//        Ble.Companion.getInstance().getBleConfig().setMethodDefaultInvokeThread(RunOn.MAIN);
        Ble.Companion.getInstance().registerObserver(eventObserver);
        Ble.Companion.getInstance().connect(device, getConnectionConfig(true), null);
        tvName.setText(device.getName());
        tvAddr.setText(device.getAddr());
    }

    private ConnectionConfig getConnectionConfig(boolean autoReconnect) {
        ConnectionConfig config = new ConnectionConfig();
        config.setDiscoverServicesDelayMillis(500);
        config.setAutoReconnect(autoReconnect);
        return config;
    }

    private EventObserver eventObserver = new SimpleEventObserver() {
        @Override
        @InvokeThread(RunOn.MAIN)
        public void onConnectionStateChanged(@NotNull Device device) {
            switch(device.getConnectionState()) {
                case IConnection.STATE_CONNECTED:	
                    tvState.setText("连接成功，等待发现服务");
            		break;
                case IConnection.STATE_CONNECTING:	
                    tvState.setText("连接中...");
            		break;
                case IConnection.STATE_DISCONNECTED:
                    tvState.setText("连接断开");
                    ToastUtils.showShort("连接断开");
                    break;
                case IConnection.STATE_SCANNING:
                    tvState.setText("正在搜索设备...");
                    break;
                case IConnection.STATE_SERVICE_DISCOVERING:
                    tvState.setText("连接成功，正在发现服务...");
                    break;
                case IConnection.STATE_SERVICE_DISCOVERED:
                    tvState.setText("连接成功，并成功发现服务");
                    Connection connection = Ble.Companion.getInstance().getConnection(device);
                    connection.setCharacteristicChangedCallback(new CharacteristicChangedCallback() {
                        @Override
                        @InvokeThread(RunOn.POSTING)
                        public void onCharacteristicChanged(@NotNull Device device, @NotNull UUID serviceUuid, @NotNull UUID characteristicUuid, @NotNull byte[] value) {
                            
                        }
                    });
                    connection.enableNotification("", UUID_SERVICE, UUID_NOTIFICATION_CHAR, new NotificationChangedCallback() {
                        @Override
                        @InvokeThread(RunOn.MAIN)
                        public void onNotificationChanged(@NotNull Device device, @NotNull String tag, @NotNull UUID serviceUuid, @NotNull UUID characteristicUuid, @NotNull UUID descriptorUuid, boolean isEnabled) {
                            Log.d("Connection", "onNotificationChanged, uiThread: " + (Looper.getMainLooper() == Looper.myLooper()));
                        }

                        @Override
                        public void onRequestFailed(@NotNull Device device, @NotNull String tag, @NotNull Request.RequestType requestType, int failType, @org.jetbrains.annotations.Nullable byte[] src) {

                        }
                    });
                    connection.writeCharacteristic("", UUID_SERVICE, UUID_RX_CHAR, new byte[]{0x5a}, new CharacteristicWriteCallback() {
                        @Override
                        public void onCharacteristicWrite(@NotNull Device device, @NotNull String tag, @NotNull UUID serviceUuid, @NotNull UUID characteristicUuid, @NotNull byte[] value) {
                            Log.d("Connection", "onCharacteristicWrite, uiThread: " + (Looper.getMainLooper() == Looper.myLooper()));
                        }

                        @Override
                        public void onRequestFailed(@NotNull Device device, @NotNull String tag, @NotNull Request.RequestType requestType, int failType, @org.jetbrains.annotations.Nullable byte[] src) {

                        }
                    });
                    connection.readRssi("", new RemoteRssiReadCallback() {
                        @Override
                        public void onRemoteRssiRead(@NotNull Device device, @NotNull String tag, int rssi) {
                            Log.d("Connection", "onRemoteRssiRead, rssi: "+rssi+", uiThread: " + (Looper.getMainLooper() == Looper.myLooper()));
                        }

                        @Override
                        public void onRequestFailed(@NotNull Device device, @NotNull String tag, @NotNull Request.RequestType requestType, int failType, @org.jetbrains.annotations.Nullable byte[] src) {

                        }
                    });
                    connection.readCharacteristic("", UUID_SERVICE_1, UUID_READ, new CharacteristicReadCallback() {
                        @Override
                        public void onCharacteristicRead(@NotNull Device device, @NotNull String tag, @NotNull UUID serviceUuid, @NotNull UUID characteristicUuid, @NotNull byte[] value) {
                            Log.d("Connection", "onCharacteristicRead, value: "+ StringUtilsKt.toHexString(value) +", uiThread: " + (Looper.getMainLooper() == Looper.myLooper()));
                        }

                        @Override
                        public void onRequestFailed(@NotNull Device device, @NotNull String tag, @NotNull Request.RequestType requestType, int failType, @org.jetbrains.annotations.Nullable byte[] src) {

                        }
                    });
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        connection.changeMtu("", 256, new MtuChangedCallback() {
                            @Override
                            public void onMtuChanged(@NotNull Device device, @NotNull String tag, int mtu) {
                                Log.d("Connection", "onMtuChanged, mtu: "+mtu+", uiThread: " + (Looper.getMainLooper() == Looper.myLooper()));
                            }

                            @Override
                            public void onRequestFailed(@NotNull Device device, @NotNull String tag, @NotNull Request.RequestType requestType, int failType, @org.jetbrains.annotations.Nullable byte[] src) {

                            }
                        });
                    }
                    connection.setCharacteristicChangedCallback(new CharacteristicChangedCallback() {
                        @Override
                        public void onCharacteristicChanged(@NotNull Device device, @NotNull UUID serviceUuid, @NotNull UUID characteristicUuid, @NotNull byte[] value) {
                            Log.d("Connection", "onCharacteristicChanged, uiThread: " + (Looper.getMainLooper() == Looper.myLooper()));
                        }
                    });
                    break;
                case IConnection.STATE_RELEASED:
                    tvState.setText("连接已释放");
                    break;
            }
            invalidateOptionsMenu();
        }

        @Override
        public void onConnectFailed(@Nullable Device device, int type) {
            tvState.setText("连接失败： " + type);
        }

        @Override
        public void onConnectTimeout(@NotNull Device device, int type) {
            switch(type) {
                case IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE:
                    ToastUtils.showShort("连接超时: 无法搜索到设备");
            		break;
                case IConnection.TIMEOUT_TYPE_CANNOT_CONNECT:
                    ToastUtils.showShort("连接超时: 无法连接设备");
                    break;
                default:
                    ToastUtils.showShort("连接超时: 无法发现蓝牙服务");
            		break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Ble.Companion.getInstance().unregisterObserver(eventObserver);
        Ble.Companion.getInstance().releaseConnection(device);//销毁连接
    }
}
