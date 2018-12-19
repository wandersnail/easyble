package com.snail.easyble.core;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.snail.easyble.callback.ConnectionStateChangeListener;
import com.snail.easyble.callback.InitCallback;
import com.snail.easyble.callback.ScanListener;
import com.snail.easyble.event.Events;

import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 描述: 蓝牙操作
 * 时间: 2018/4/11 15:29
 * 作者: zengfansheng
 */
public class Ble {
    private BluetoothAdapter bluetoothAdapter;
    private Map<String, Connection> connectionMap;
    private boolean isInited;
    private boolean scanning;
    private BluetoothLeScanner bleScanner;
    private ScanCallback scanCallback;
    private BluetoothAdapter.LeScanCallback leScanCallback;
    private BleConfig bleConfig;
    private List<ScanListener> scanListeners;
    private Handler mainThreadHandler;
    private EventBus publisher;
    private BleLogger logger;
    private ExecutorService executorService;
    private Application app;

    private Ble() {
        bleConfig = new BleConfig();
        connectionMap = new ConcurrentHashMap<>();
        mainThreadHandler = new Handler(Looper.getMainLooper());
        scanListeners = new ArrayList<>();
        publisher = EventBus.builder().build();
        logger = new BleLogger();
        executorService = Executors.newCachedThreadPool();
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                tryGetContext();
            }
        });
    }

    private static class Holder {
        private static final Ble BLE = new Ble();
    }

    public static Ble getInstance() {
        return Holder.BLE;
    }

    private void tryGetContext() {
        try {
            Class<?> clazz = Class.forName("android.app.ActivityThread");
            Method acThreadMethod = clazz.getMethod("currentActivityThread");
            acThreadMethod.setAccessible(true);
            Object acThread = acThreadMethod.invoke(null);
            Method appMethod = acThread.getClass().getMethod("getApplication");
            app = (Application) appMethod.invoke(acThread);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Context getContext() {
        if (app == null) {
            tryGetContext();
            if (app != null) {
                initialize(app);
            }
        }
        return app;
    }

    /**
     * 设置日志输出级别控制，与{@link #setLogPrintFilter(BleLogger.Filter)}同时作用
     *
     * @param logPrintLevel <br>{@link BleLogger#NONE}, {@link BleLogger#VERBOSE},
     *                      {@link BleLogger#DEBUG}, {@link BleLogger#INFO}, {@link BleLogger#WARN}, {@link BleLogger#ERROR}
     */
    public void setLogPrintLevel(int logPrintLevel) {
        logger.setPrintLevel(logPrintLevel);
    }

    /**
     * 设置日志输出过滤器，与{@link #setLogPrintLevel(int)}同时作用
     */
    public void setLogPrintFilter(BleLogger.Filter filter) {
        logger.setFilter(filter);
    }

    public static void println(Class cls, int priority, @NonNull String msg) {
        Ble.getInstance().postEvent(Events.newLogChanged(msg, BleLogger.getLevel(priority)));
        Ble.getInstance().logger.println("blelib:" + cls.getSimpleName(), priority, msg);
    }

    public BleConfig getBleConfig() {
        return bleConfig;
    }

    /**
     * 替换默认配置
     */
    public void setBleConfig(@NonNull BleConfig bleConfig) {
        this.bleConfig = bleConfig;
    }

    /**
     * 必须先初始化，只需一次
     *
     * @param app 上下文
     */
    public synchronized boolean initialize(@NonNull Application app) {
        if (isInited) {
            return true;
        }
        //检查手机是否支持BLE
        if (!app.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false;
        }
        //获取蓝牙管理器
        BluetoothManager bluetoothManager = (BluetoothManager) app.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null || bluetoothManager.getAdapter() == null) {
            return false;
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        //监听蓝牙开关变化
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        app.registerReceiver(receiver, filter);
        isInited = true;
        return true;
    }

    /**
     * 必须先初始化
     *
     * @param context  上下文，需是Application
     * @param callback 初始化结果回调
     * @deprecated 此方法后续会删除，使用 {@link #initialize(Application)}替代
     */
    @Deprecated
    public synchronized void initialize(@NonNull Context context, final InitCallback callback) {
        if (isInited) {
            if (callback != null) {
                mainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess();
                    }
                });
            }
            return;
        }
        if (app == null) {
            if (context instanceof Application) {
                app = (Application) context;
            } else {
                throw new IllegalArgumentException("Parameter context must be the instance of Application!");
            }
        }
        //检查手机是否支持BLE
        if (!app.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            if (callback != null) {
                mainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFail(InitCallback.ERROR_NOT_SUPPORT_BLE);
                    }
                });
            }
            return;
        }
        //获取蓝牙管理器
        BluetoothManager bluetoothManager = (BluetoothManager) app.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null || bluetoothManager.getAdapter() == null) {
            if (callback != null) {
                mainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFail(InitCallback.ERROR_INIT_FAIL);
                    }
                });
            }
            return;
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        //监听蓝牙开关变化
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        app.registerReceiver(receiver, filter);
        isInited = true;
        if (callback != null) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess();
                }
            });
        }
    }

    private boolean checkInitStateAndContext() {
        if (!isInited) {
            if (!tryAutoInit()) {
                new Exception("The SDK has not been initialized, make sure to call Ble.getInstance().initialize(Application) first.").printStackTrace();
                return false;
            }
        } else if (app == null) {
            return tryAutoInit();
        }
        return true;
    }

    private boolean tryAutoInit() {
        tryGetContext();
        if (app != null) {
            initialize(app);
        }
        return isInited;
    }

    /**
     * 关闭所有连接，释放资源
     */
    public void release() {
        if (checkInitStateAndContext()) {
            stopScan();
            scanListeners.clear();
            releaseAllConnections();//释放所有连接
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {//蓝牙开关状态变化                 
                if (bluetoothAdapter != null) {
                    publisher.post(Events.newBluetoothStateChanged(bluetoothAdapter.getState()));
                    if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {//蓝牙关闭了
                        scanning = false;
                        handleScanCallback(false, null, -1, "");
                        //主动断开
                        for (Connection connection : connectionMap.values()) {
                            connection.disconnect();
                        }
                    } else if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                        for (Connection connection : connectionMap.values()) {
                            if (connection.isAutoReconnectEnabled()) {
                                connection.reconnect();//如果开启了自动重连，则重连
                            }
                        }
                    }
                }
            }
        }
    };

    //是否缺少定位权限
    private boolean noLocationPermission() {
        return (ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(app,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED);
    }

    //判断位置服务是否打开
    private boolean isLocationEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager locationManager = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                return locationManager.isLocationEnabled();
            }
        } else {
            try {
                int locationMode = Settings.Secure.getInt(app.getContentResolver(), Settings.Secure.LOCATION_MODE);
                return locationMode != Settings.Secure.LOCATION_MODE_OFF;
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 是否已初始化过或上下文为空了，需要重新初始化了
     */
    public boolean isInitialized() {
        return isInited && app != null;
    }

    public void postEvent(@NonNull Object event) {
        publisher.post(event);
    }

    public void postEventOnBackground(@NonNull Object event) {
        executorService.execute(new EventRunnable(event));
    }

    private class EventRunnable implements Runnable {
        private Object event;

        EventRunnable(Object event) {
            this.event = event;
        }

        @Override
        public void run() {
            publisher.post(event);
        }
    }

    /**
     * 注册订阅者，开始监听蓝牙状态及数据
     *
     * @param subscriber 订阅者
     */
    public void registerSubscriber(@NonNull Object subscriber) {
        if (!publisher.isRegistered(subscriber)) {
            publisher.register(subscriber);
        }
    }

    /**
     * 取消注册订阅者，停止监听蓝牙状态及数据
     *
     * @param subscriber 订阅者
     */
    public void unregisterSubscriber(@NonNull Object subscriber) {
        publisher.unregister(subscriber);
    }

    /**
     * 添加扫描监听器
     */
    public void addScanListener(ScanListener listener) {
        if (listener != null && !scanListeners.contains(listener)) {
            scanListeners.add(listener);
        }
    }

    /**
     * 移除扫描监听器
     */
    public void removeScanListener(ScanListener listener) {
        scanListeners.remove(listener);
    }

    /**
     * 是否正在扫描
     */
    public boolean isScanning() {
        return scanning;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public boolean isBluetoothAdapterEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * 搜索蓝牙设备
     */
    public void startScan() {
        synchronized (this) {
            if (!checkInitStateAndContext() || bluetoothAdapter == null || !bluetoothAdapter.isEnabled() || scanning) {
                return;
            }
            if (!isLocationEnabled()) {
                handleScanCallback(false, null, ScanListener.ERROR_LOCATION_SERVICE_CLOSED, "位置服务未开启，无法搜索蓝牙设备");
                return;
            } else if (noLocationPermission()) {
                handleScanCallback(false, null, ScanListener.ERROR_LACK_LOCATION_PERMISSION, "缺少定位权限，无法搜索蓝牙设备");
                return;
            }
            scanning = true;
        }
        handleScanCallback(true, null, -1, "");
        if (bleConfig.isAcceptSysConnectedDevice()) {
            getSystemConnectedDevices();
        }
        //如果是高版本使用新的搜索方法
        if (bleConfig.isUseBluetoothLeScanner() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (bleScanner == null) {
                bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
            if (scanCallback == null) {
                scanCallback = new MyScanCallback();
            }
            if (bleConfig.getScanSettings() == null) {
                bleScanner.startScan(scanCallback);
            } else {
                bleScanner.startScan(null, bleConfig.getScanSettings(), scanCallback);
            }
        } else {
            if (leScanCallback == null) {
                leScanCallback = new MyLeScanCallback();
            }
            bluetoothAdapter.startLeScan(leScanCallback);
        }
        mainThreadHandler.postDelayed(stopScanRunnable, bleConfig.getScanPeriodMillis());
    }

    private void handleScanCallback(final boolean start, final Device device, final int errorCode, final String errorMsg) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                for (ScanListener listener : scanListeners) {
                    if (device != null) {
                        listener.onScanResult(device);
                    } else if (start) {
                        listener.onScanStart();
                    } else if (errorCode >= 0) {
                        listener.onScanError(errorCode, errorMsg);
                    } else {
                        listener.onScanStop();
                    }
                }
            }
        });
    }

    private Runnable stopScanRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };

    //获取系统已连接的设备
    private void getSystemConnectedDevices() {
        try {
            //得到连接状态的方法
            Method method = bluetoothAdapter.getClass().getDeclaredMethod("getConnectionState");
            //打开权限  
            method.setAccessible(true);
            int state = (int) method.invoke(bluetoothAdapter);
            if (state == BluetoothAdapter.STATE_CONNECTED) {
                Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : devices) {
                    Method isConnectedMethod = device.getClass().getDeclaredMethod("isConnected");
                    method.setAccessible(true);
                    boolean isConnected = (boolean) isConnectedMethod.invoke(device);
                    if (isConnected) {
                        parseScanResult(device, 0, null);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止搜索蓝牙设备
     */
    public void stopScan() {
        if (!checkInitStateAndContext() || !scanning) {
            return;
        }
        scanning = false;
        mainThreadHandler.removeCallbacks(stopScanRunnable);
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (bleScanner != null && scanCallback != null) {
                bleScanner.stopScan(scanCallback);
            }
        }
        if (leScanCallback != null) {
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
        handleScanCallback(false, null, -1, "");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class MyScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            ScanRecord scanRecord = result.getScanRecord();
            parseScanResult(result.getDevice(), result.getRssi(), scanRecord == null ? null : scanRecord.getBytes());
        }
    }

    private class MyLeScanCallback implements BluetoothAdapter.LeScanCallback {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            parseScanResult(device, rssi, scanRecord);
        }
    }

    /**
     * 解析广播字段
     *
     * @param device     蓝牙设备
     * @param rssi       信号强度
     * @param scanRecord 广播内容
     */
    public void parseScanResult(@NonNull BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (bleConfig.isHideNonBleDevice() && device.getType() != BluetoothDevice.DEVICE_TYPE_LE) {
            return;
        }
        for (Connection connection : connectionMap.values()) {
            connection.onScanResult(device.getAddress());
        }
        String deviceName = TextUtils.isEmpty(device.getName()) ? "Unknown Device" : device.getName();
        //生成
        Device dev = null;
        if (bleConfig.getScanHandler() != null) {
            //只在指定的过滤器通知
            dev = bleConfig.getScanHandler().handle(device, scanRecord);
        }
        if (dev != null || bleConfig.getScanHandler() == null) {
            if (dev == null) {
                dev = new Device();
            }
            dev.name = TextUtils.isEmpty(dev.name) ? deviceName : dev.name;
            dev.addr = device.getAddress();
            dev.rssi = rssi;
            dev.bondState = device.getBondState();
            dev.originalDevice = device;
            dev.scanRecord = scanRecord;
            handleScanCallback(false, dev, -1, "");
        }
        println(Ble.class, Log.DEBUG, String.format(Locale.US, "found device! [name: %s, mac: %s]", deviceName, device.getAddress()));
    }

    /**
     * 建立连接
     *
     * @param config 连接配置 {@link BluetoothDevice#TRANSPORT_AUTO}<br>{@link BluetoothDevice#TRANSPORT_BREDR}<br>{@link BluetoothDevice#TRANSPORT_LE}
     */
    public synchronized void connect(@NonNull String addr, ConnectionConfig config, ConnectionStateChangeListener listener) {
        if (!checkInitStateAndContext()) {
            return;
        }
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(addr);
        if (device == null) {
            Connection.notifyConnectFailed(null, IConnection.CONNECT_FAIL_TYPE_UNSPECIFIED_MAC_ADDRESS, listener);
        } else {
            connect(Device.valueOf(device), config, listener);
        }
    }
    
    /**
     * 建立连接
     *
     * @param config 连接配置 {@link BluetoothDevice#TRANSPORT_AUTO}<br>{@link BluetoothDevice#TRANSPORT_BREDR}<br>{@link BluetoothDevice#TRANSPORT_LE}
     */
    public synchronized void connect(@NonNull Device device, ConnectionConfig config, ConnectionStateChangeListener listener) {
        if (!checkInitStateAndContext()) {
            return;
        }
        Connection connection = connectionMap.remove(device.addr);
        //此前这个设备建立过连接，销毁之前的连接重新创建
        if (connection != null) {
            connection.releaseNoEvnet();
        }
        IBondController bondController = bleConfig.getBondController();
        if (bondController != null && bondController.bond(device)) {
            BluetoothDevice bd = bluetoothAdapter.getRemoteDevice(device.addr);
            if (bd.getBondState() == BluetoothDevice.BOND_BONDED) {
                connection = Connection.newInstance(bluetoothAdapter, device, config, 0, listener);
            } else {
                createBond(device.addr);//配对
                connection = Connection.newInstance(bluetoothAdapter, device, config, 1500, listener);
            }
        } else {
            connection = Connection.newInstance(bluetoothAdapter, device, config, 0, listener);
        }
        if (connection != null) {
            connectionMap.put(device.addr, connection);
        }
    }

    /**
     * 获取连接
     */
    public Connection getConnection(Device device) {
        return device == null ? null : connectionMap.get(device.addr);
    }

    /**
     * 获取连接
     */
    public Connection getConnection(String addr) {
        return addr == null ? null : connectionMap.get(addr);
    }

    /**
     * 获取连接状态
     *
     * @return {@link Connection#STATE_DISCONNECTED}<br> {@link Connection#STATE_CONNECTING}<br>
     * {@link Connection#STATE_SCANNING}<br> {@link Connection#STATE_CONNECTED}<br>
     * {@link Connection#STATE_SERVICE_DISCOVERING}<br> {@link Connection#STATE_SERVICE_DISCOVERED}
     */
    public int getConnectionState(Device device) {
        Connection connection = getConnection(device);
        return connection == null ? Connection.STATE_DISCONNECTED : connection.getConnctionState();
    }

    /**
     * 根据设备断开其连接
     */
    public void disconnectConnection(Device device) {
        if (checkInitStateAndContext() && device != null) {
            Connection connection = connectionMap.get(device.addr);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 断开所有连接
     */
    public void disconnectAllConnection() {
        for (Connection connection : connectionMap.values()) {
            connection.disconnect();
        }
    }

    /**
     * 释放所有创建的连接
     */
    public synchronized void releaseAllConnections() {
        for (Connection connection : connectionMap.values()) {
            connection.release();
        }
        connectionMap.clear();
    }

    /**
     * 根据设备释放连接
     */
    public synchronized void releaseConnection(Device device) {
        if (checkInitStateAndContext() && device != null) {
            Connection connection = connectionMap.remove(device.addr);
            if (connection != null) {
                connection.release();
            }
        }
    }

    /**
     * 重连所有创建的连接
     */
    public void reconnectAll() {
        if (checkInitStateAndContext()) {
            for (Connection connection : connectionMap.values()) {
                if (connection.getConnctionState() != Connection.STATE_SERVICE_DISCOVERED) {
                    connection.reconnect();
                }
            }
        }
    }

    /**
     * 根据设备重连
     */
    public void reconnect(Device device) {
        if (checkInitStateAndContext() && device != null) {
            Connection connection = connectionMap.get(device.addr);
            if (connection != null && connection.getConnctionState() != Connection.STATE_SERVICE_DISCOVERED) {
                connection.reconnect();
            }
        }
    }

    /**
     * 设置是否可自动重连，所有已创建的连接
     */
    public void setAutoReconnectEnable(boolean enable) {
        for (Connection connection : connectionMap.values()) {
            connection.setAutoReconnectEnable(enable);
        }
    }

    /**
     * 设置是否可自动重连
     */
    public void setAutoReconnectEnable(Device device, boolean enable) {
        if (device != null) {
            Connection connection = connectionMap.get(device.addr);
            if (connection != null) {
                connection.setAutoReconnectEnable(enable);
            }
        }
    }

    /**
     * 刷新设备，清除缓存
     */
    public void refresh(Device device) {
        if (checkInitStateAndContext() && device != null) {
            Connection connection = connectionMap.get(device.addr);
            if (connection != null) {
                connection.refresh();
            }
        }
    }

    /**
     * 获取设备配对状态
     *
     * @return {@link BluetoothDevice#BOND_NONE},
     * {@link BluetoothDevice#BOND_BONDING},
     * {@link BluetoothDevice#BOND_BONDED}.
     */
    public int getBondState(String addr) {
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(addr);
            return device.getBondState();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return BluetoothDevice.BOND_NONE;
    }

    /**
     * 绑定设备
     */
    public boolean createBond(String addr) {
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(addr);
            return device.getBondState() != BluetoothDevice.BOND_NONE || device.createBond();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 根据设置的过滤器清除已配对的设备
     */
    public void clearBondDevices(RemoveBondFilter filter) {
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            if (filter == null || filter.accept(device)) {
                try {
                    device.getClass().getMethod("removeBond").invoke(device);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 取消配对
     */
    public void removeBond(String addr) {
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(addr);
            if (device.getBondState() != BluetoothDevice.BOND_NONE) {
                device.getClass().getMethod("removeBond").invoke(device);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
