package com.snail.easyble.core;

import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import java.util.Queue;
import java.util.UUID;

import com.snail.easyble.callback.RequestCallback;
import com.snail.easyble.util.BleUtils;

/**
 * 描述: 用作请求队列
 * 时间: 2018/4/11 15:15
 * 作者: zengfansheng
 */
public class Request {
    
    public enum RequestType {
        TOGGLE_NOTIFICATION, TOGGLE_INDICATION, READ_CHARACTERISTIC, READ_DESCRIPTOR, READ_RSSI, WRITE_CHARACTERISTIC, CHANGE_MTU
    }

    public RequestType type;
    public UUID service;
    public UUID characteristic;
    public UUID descriptor;
    public String requestId;
    public byte[] value;
    boolean waitWriteResult;
    int writeDelay;
    //-----分包发送时用到-----
    Queue<byte[]> remainQueue;
    byte[] sendingBytes;
    //----------------------
    RequestCallback callback;
    
    private Request(@NonNull RequestType type, @NonNull String requestId, UUID service, UUID characteristic, UUID descriptor, byte[] value, RequestCallback callback) {
        this.type = type;
        this.requestId = requestId;
        this.service = service;
        this.characteristic = characteristic;
        this.descriptor = descriptor;
        this.value = value;
        this.callback = callback;
    }
    
    static Request newChangeMtuRequest(@NonNull String requestId, int mtu) {
        return newChangeMtuRequest(requestId, mtu, null);
    }

    static Request newChangeMtuRequest(@NonNull String requestId, int mtu, RequestCallback callback) {
        if (mtu < 23) {
            mtu = 23;
        } else if (mtu > 517) {
            mtu = 517;
        }
        return new Request(RequestType.CHANGE_MTU, requestId, null, null, null, BleUtils.numberToBytes(false, mtu, 4), callback);
    }
    
    static Request newReadCharacteristicRequest(@NonNull String requestId, UUID service, UUID characteristic) {
        return newReadCharacteristicRequest(requestId, service, characteristic, null);
    }

    static Request newReadCharacteristicRequest(@NonNull String requestId, UUID service, UUID characteristic, RequestCallback callback) {
        return new Request(RequestType.READ_CHARACTERISTIC, requestId, service, characteristic, null, null, callback);
    }
    
    static Request newToggleNotificationRequest(@NonNull String requestId, UUID service, UUID characteristic, boolean enable) {
        return newToggleNotificationRequest(requestId, service, characteristic, enable, null);
    }

    static Request newToggleNotificationRequest(@NonNull String requestId, UUID service, UUID characteristic, boolean enable, RequestCallback callback) {
        return new Request(RequestType.TOGGLE_NOTIFICATION, requestId, service, characteristic, null,
                enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, callback);
    }

    static Request newToggleIndicationRequest(@NonNull String requestId, UUID service, UUID characteristic, boolean enable) {
        return newToggleIndicationRequest(requestId, service, characteristic, enable, null);
    }

    static Request newToggleIndicationRequest(@NonNull String requestId, UUID service, UUID characteristic, boolean enable, RequestCallback callback) {
        return new Request(RequestType.TOGGLE_INDICATION, requestId, service, characteristic, null,
                enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, callback);
    }
    
    static Request newReadDescriptorRequest(@NonNull String requestId, UUID service, UUID characteristic, UUID descriptor) {
        return newReadDescriptorRequest(requestId, service, characteristic, descriptor, null);
    }

    static Request newReadDescriptorRequest(@NonNull String requestId, UUID service, UUID characteristic, UUID descriptor, RequestCallback callback) {
        return new Request(RequestType.READ_DESCRIPTOR, requestId, service, characteristic, descriptor, null, callback);
    }
    
    static Request newWriteCharacteristicRequest(@NonNull String requestId, UUID service, UUID characteristic, byte[] value) {
        return newWriteCharacteristicRequest(requestId, service, characteristic, value, null);
    }

    static Request newWriteCharacteristicRequest(@NonNull String requestId, UUID service, UUID characteristic, byte[] value, RequestCallback callback) {
        return new Request(RequestType.WRITE_CHARACTERISTIC, requestId, service, characteristic, null, value, callback);
    }
    
    static Request newReadRssiRequest(@NonNull String requestId) {
        return newReadRssiRequest(requestId, null);
    }

    static Request newReadRssiRequest(@NonNull String requestId, RequestCallback callback) {
        return new Request(RequestType.READ_RSSI, requestId, null, null, null, null, callback);
    }
}
