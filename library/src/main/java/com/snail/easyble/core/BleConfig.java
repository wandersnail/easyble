package com.snail.easyble.core;

import android.bluetooth.le.ScanSettings;

/**
 * 描述:
 * 时间: 2018/8/5 18:28
 * 作者: zengfansheng
 */
public class BleConfig {
    private IScanHandler scanHandler;
    private IBondController bondController;
    private int scanPeriodMillis = 10000;
    private boolean useBluetoothLeScanner = true;
    private boolean acceptSysConnectedDevice;
    private ScanSettings scanSettings;
    private boolean hideNonBleDevice;

    /**
     * 设置扫描过滤器
     *
     * @param handler 扫描结果处理
     */
    public BleConfig setScanHandler(IScanHandler handler) {
        scanHandler = handler;
        return this;
    }

    public IScanHandler getScanHandler() {
        return scanHandler;
    }

    public int getScanPeriodMillis() {
        return scanPeriodMillis;
    }

    /**
     * 设置蓝牙扫描周期
     *
     * @param scanPeriodMillis 毫秒
     */
    public BleConfig setScanPeriodMillis(int scanPeriodMillis) {
        this.scanPeriodMillis = scanPeriodMillis;
        return this;
    }

    public boolean isUseBluetoothLeScanner() {
        return useBluetoothLeScanner;
    }

    /**
     * 控制是否使用新版的扫描器
     */
    public BleConfig setUseBluetoothLeScanner(boolean useBluetoothLeScanner) {
        this.useBluetoothLeScanner = useBluetoothLeScanner;
        return this;
    }

    public IBondController getBondController() {
        return bondController;
    }

    /**
     * 连接进行配对的控制
     */
    public BleConfig setBondController(IBondController bondController) {
        this.bondController = bondController;
        return this;
    }

    public boolean isAcceptSysConnectedDevice() {
        return acceptSysConnectedDevice;
    }

    public BleConfig setAcceptSysConnectedDevice(boolean acceptSysConnectedDevice) {
        this.acceptSysConnectedDevice = acceptSysConnectedDevice;
        return this;
    }

    public ScanSettings getScanSettings() {
        return scanSettings;
    }

    /**
     * 扫描设置
     */
    public BleConfig setScanSettings(ScanSettings scanSettings) {
        this.scanSettings = scanSettings;
        return this;
    }

    public boolean isHideNonBleDevice() {
        return hideNonBleDevice;
    }

    /**
     * 设置是否过滤非BLE设备
     */
    public void setHideNonBleDevice(boolean hideNonBleDevice) {
        this.hideNonBleDevice = hideNonBleDevice;
    }
}
