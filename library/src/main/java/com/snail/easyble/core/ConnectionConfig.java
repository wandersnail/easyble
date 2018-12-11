package com.snail.easyble.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 描述:
 * 时间: 2018/8/5 19:28
 * 作者: zengfansheng
 */
public class ConnectionConfig implements Cloneable {
    public static final int TRY_RECONNECT_TIMES_INFINITE = -1;//无限重连

    long discoverServicesDelayMillis = 500;
    int connectTimeoutMillis = 10000;//连接超时时间
    int requestTimeoutMillis = 3000;//请求超时时间
    int tryReconnectTimes = TRY_RECONNECT_TIMES_INFINITE;
    int packageWriteDelayMillis;
    int requestWriteDelayMillis = -1;
    int packageSize = 20;//发送数据时的分包大小
    boolean waitWriteResult = true;
    int reconnectImmediatelyTimes = 3;//不搜索，直接通过mac连接的最大连接次数
    private Map<String, Integer> writeTypeMap = new HashMap<>();
    boolean autoReconnect = true;//是否自动重连
    int transport = -1;//传输模式

    private ConnectionConfig() {
        
    }
    
    public static ConnectionConfig newInstance() {
        return new ConnectionConfig();
    }
    
    /**
     * 获取连接超时时间
     */
    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    /**
     * 设置连接超时时间
     */
    public ConnectionConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        return this;
    }

    /**
     * 设置连接成功后，延时发现服务的时间
     *
     * @param delayMillis 延时，毫秒
     */
    public ConnectionConfig setDiscoverServicesDelayMillis(int delayMillis) {
        this.discoverServicesDelayMillis = delayMillis;
        return this;
    }

    public long getDiscoverServicesDelayMillis() {
        return discoverServicesDelayMillis;
    }

    public int getTryReconnectTimes() {
        return tryReconnectTimes;
    }

    /**
     * 设置断开后尝试自动重连次数。-1为无限重连。默认为-1
     */
    public ConnectionConfig setTryReconnectTimes(int tryReconnectTimes) {
        this.tryReconnectTimes = tryReconnectTimes;
        return this;
    }

    public int getPackageWriteDelayMillis() {
        return packageWriteDelayMillis;
    }

    /**
     * 设置包发送延时，默认不延时
     */
    public ConnectionConfig setPackageWriteDelayMillis(int packageWriteDelayMillis) {
        this.packageWriteDelayMillis = packageWriteDelayMillis;
        return this;
    }

    public int getRequestWriteDelayMillis() {
        return requestWriteDelayMillis;
    }

    /**
     * 设置写请求延时，如写请求需要分包，每包发送时的延时是包延时，而非此延时。默认不延时
     */
    public void setRequestWriteDelayMillis(int requestWriteDelayMillis) {
        this.requestWriteDelayMillis = requestWriteDelayMillis;
    }



    public int getPackageSize() {
        return packageSize;
    }

    /**
     * 发送数据时的分包大小
     *
     * @param packageSize 包大小，字节
     */
    public ConnectionConfig setPackageSize(int packageSize) {
        if (packageSize > 0) {
            this.packageSize = packageSize;
        }
        return this;
    }

    public Integer getWriteType(UUID service, UUID characteristic) {
        return writeTypeMap.get(String.format(Locale.US, "%s:%s", service.toString(), characteristic.toString()));
    }

    /**
     * 设置写入模式，默认的规则：如果Characteristic的属性有PROPERTY_WRITE_NO_RESPONSE则使用
     * {@link BluetoothGattCharacteristic#WRITE_TYPE_NO_RESPONSE}，否则使用{@link BluetoothGattCharacteristic#WRITE_TYPE_DEFAULT}
     *
     * @param writeType {@link BluetoothGattCharacteristic#WRITE_TYPE_NO_RESPONSE}<br>{@link BluetoothGattCharacteristic#WRITE_TYPE_DEFAULT}<br>
     *                  {@link BluetoothGattCharacteristic#WRITE_TYPE_SIGNED}
     */
    public ConnectionConfig setWriteType(UUID service, UUID characteristic, int writeType) {
        writeTypeMap.put(String.format(Locale.US, "%s:%s", service.toString(), characteristic.toString()), writeType);
        return this;
    }

    public boolean isWaitWriteResult() {
        return waitWriteResult;
    }

    /**
     * 是否等待写入结果，不等待则直接处理下一个请求，否则等待onCharacteristicWrite回调后再处理下一请求，默认等待。<br>
     * 不等待的话onCharacteristicWrite回调不会处理，而是在writeCharacteristic发布onCharacteristicWrite信息
     */
    public ConnectionConfig setWaitWriteResult(boolean waitWriteResult) {
        this.waitWriteResult = waitWriteResult;
        return this;
    }

    public int getReconnectImmediatelyTimes() {
        return reconnectImmediatelyTimes;
    }

    /**
     * 连接失败或连接断开时，不搜索，直接通过mac尝试重连
     * @param reconnectImmediatelyTimes 最大尝试次数。超过此次数后进行搜索重连
     */
    public void setReconnectImmediatelyTimes(int reconnectImmediatelyTimes) {
        this.reconnectImmediatelyTimes = reconnectImmediatelyTimes;
    }

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    /**
     * 是否断线自动重连
     */
    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    public int getTransport() {
        return transport;
    }

    /**
     * @param transport 连接时的传输模式，只在6.0以上系统有效。
     * {@link BluetoothDevice#TRANSPORT_AUTO}<br>{@link BluetoothDevice#TRANSPORT_BREDR}<br>{@link BluetoothDevice#TRANSPORT_LE} 
     */
    public void setTransport(int transport) {
        this.transport = transport;
    }

    public int getRequestTimeoutMillis() {
        return requestTimeoutMillis;
    }

    /**
     * 设置请求超时时间
     */
    public void setRequestTimeoutMillis(int requestTimeoutMillis) {
        if (requestTimeoutMillis > 1000) {
            this.requestTimeoutMillis = requestTimeoutMillis;
        }
    }
}
