package com.snail.easyble.core;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import java.util.UUID;

/**
 * 描述:
 * 时间: 2018/6/23 18:58
 * 作者: zengfansheng
 */
public interface IConnection {
    UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /**普通请求失败*/
    int REQUEST_FAIL_TYPE_REQUEST_FAILED = 0;
    /**特征值UUID为空*/
    int REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC = 1;
    /**descriptor的UUID为空*/
    int REQUEST_FAIL_TYPE_NULL_DESCRIPTOR = 2;
    /**服务UUID为空*/
    int REQUEST_FAIL_TYPE_NULL_SERVICE = 3;
    /** 请求的回调状态不是{@link BluetoothGatt#GATT_SUCCESS} */
    int REQUEST_FAIL_TYPE_GATT_STATUS_FAILED = 4;
    /**GATT实例为空*/
    int REQUEST_FAIL_TYPE_GATT_IS_NULL = 5;
    /**API级别太低*/
    int REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW = 6;
    /**蓝牙未开启*/
    int REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED = 7;
    /**请求超时*/
    int REQUEST_FAIL_TYPE_REQUEST_TIMEOUT = 8;
    /**连接已断开*/
    int REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED = 9;
    /**连接已释放*/
    int REQUEST_FAIL_TYPE_CONNECTION_RELEASED = 10;
    /**写入的值为空*/
    int REQUEST_FAIL_TYPE_VALUE_IS_NULL_OR_EMPTY = 11;

    //----------蓝牙连接状态-------------   
    /**连接断开*/
    int STATE_DISCONNECTED = 0;
    /**正在连接*/
    int STATE_CONNECTING = 1;
    /**正在搜索设备*/
    int STATE_SCANNING = 2;
    /**已连接，待发现服务*/
    int STATE_CONNECTED = 3;
    /**已连接，正在发现服务*/
    int STATE_SERVICE_DISCOVERING = 4;
    /**已连接，并已发现服务*/
    int STATE_SERVICE_DISCOVERED = 5;
    /**连接已释放*/
    int STATE_RELEASED = 6;
    //----------连接超时类型---------
    /**搜索不到设备*/
    int TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE = 0;
    /**能搜到，连接不上*/
    int TIMEOUT_TYPE_CANNOT_CONNECT = 1;
    /**能连接上，无法发现服务*/
    int TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES = 2;

    //连接失败类型
    /** 非法的设备MAC地址 */
    int CONNECT_FAIL_TYPE_UNSPECIFIED_MAC_ADDRESS = 1;
    /** 达到最大重连次数 */
    int CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION = 2;

    void onCharacteristicRead(@NonNull String requestId, BluetoothGattCharacteristic characteristic);

    void onCharacteristicChanged(BluetoothGattCharacteristic characteristic);

    void onReadRemoteRssi(@NonNull String requestId, int rssi);

    void onMtuChanged(@NonNull String requestId, int mtu);

    void onRequestFialed(@NonNull String requestId, @NonNull Request.RequestType requestType, int failType, byte[] value);

    void onDescriptorRead(@NonNull String requestId, BluetoothGattDescriptor descriptor);

    void onNotificationChanged(@NonNull String requestId, BluetoothGattDescriptor descriptor, boolean isEnabled);

    void onIndicationChanged(@NonNull String requestId, BluetoothGattDescriptor descriptor, boolean isEnabled);

    void onCharacteristicWrite(@NonNull String requestId, GattCharacteristic characteristic);
}
