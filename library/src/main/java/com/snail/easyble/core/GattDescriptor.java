package com.snail.easyble.core;

import android.support.annotation.NonNull;

import java.util.UUID;

/**
 * 描述:
 * 时间: 2018/7/24 10:01
 * 作者: zengfansheng
 */
public class GattDescriptor {
    public UUID serviceUuid;
    public UUID characteristicUuid;
    public UUID descriptorUuid;
    public byte[] value;

    public GattDescriptor(@NonNull UUID serviceUuid, @NonNull UUID characteristicUuid, @NonNull UUID descriptorUuid, byte[] value) {
        this.serviceUuid = serviceUuid;
        this.characteristicUuid = characteristicUuid;
        this.descriptorUuid = descriptorUuid;
        this.value = value;
    }
}
