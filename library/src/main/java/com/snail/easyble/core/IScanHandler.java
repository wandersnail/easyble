package com.snail.easyble.core;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

/**
 * 描述: 蓝牙搜索过滤器
 * 时间: 2018/4/13 10:42
 * 作者: zengfansheng
 */
public interface IScanHandler {
    /**
     * 根据广播信息添加设备属性并确定是否过滤
     * @param device 搜索到的设备
     * @param scanRecord 广播内容
     * @return 是否过滤，null时表示不过滤
     */
    Device handle(@NonNull BluetoothDevice device, byte[] scanRecord);
}
