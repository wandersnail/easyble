package com.snail.easyble.event;

import android.support.annotation.NonNull;

import com.snail.easyble.core.Device;

/**
 * 描述: 带设备的事件
 * 时间: 2018/5/19 18:57
 * 作者: zengfansheng
 */
public class DeviceEvent<D extends Device> {
    /** 设备 */
    public @NonNull D device;

    public DeviceEvent(@NonNull D device) {
        this.device = device;
    }
}
