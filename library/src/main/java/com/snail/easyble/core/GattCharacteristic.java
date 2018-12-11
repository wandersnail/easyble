package com.snail.easyble.core;

import android.support.annotation.NonNull;

import java.util.UUID;

/**
 * 描述:
 * 时间: 2018/7/24 09:56
 * 作者: zengfansheng
 */
public class GattCharacteristic {
    public UUID serviceUuid;
    public UUID characteristicUuid;
    public byte[] value;
    
    public GattCharacteristic(@NonNull UUID serviceUuid, @NonNull UUID characteristicUuid, byte[] value) {
        this.serviceUuid = serviceUuid;
        this.characteristicUuid = characteristicUuid;
        this.value = value;
    }
}
