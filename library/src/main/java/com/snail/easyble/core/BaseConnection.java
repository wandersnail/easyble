package com.snail.easyble.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;

import com.snail.easyble.annotation.InvokeThread;
import com.snail.easyble.annotation.RunOn;
import com.snail.easyble.callback.CharacteristicChangedCallback;
import com.snail.easyble.callback.RequestCallback;
import com.snail.easyble.event.Events;
import com.snail.easyble.util.BleUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 描述: 蓝牙连接基类
 * 时间: 2018/4/11 16:37
 * 作者: zengfansheng
 */
public abstract class BaseConnection extends BluetoothGattCallback implements IConnection {
    private static final int MSG_REQUEST_TIMEOUT = 0;
    protected static final int MSG_CONNECT = 1;
    protected static final int MSG_DISCONNECT = 2;
    protected static final int MSG_REFRESH= 3;
    protected static final int MSG_AUTO_REFRESH = 4;
    protected static final int MSG_TIMER = 5;
    protected static final int MSG_RELEASE = 6;
    protected static final int MSG_DISCOVER_SERVICES = 7;
    protected static final int MSG_ON_CONNECTION_STATE_CHANGE = 8;
    protected static final int MSG_ON_SERVICES_DISCOVERED = 9;
    
    protected BluetoothDevice bluetoothDevice;
    protected BluetoothGatt bluetoothGatt;
    protected List<Request> requestQueue = new ArrayList<>();
    protected Request currentRequest;
    private BluetoothGattCharacteristic pendingCharacteristic;
    protected BluetoothAdapter bluetoothAdapter;
    protected boolean isReleased;
    Handler connHandler;
    private Handler mainHandler;
    protected ConnectionConfig config;
    private CharacteristicChangedCallback characteristicChangedCallback;
    protected Device device;
    private ExecutorService executorService;

    BaseConnection(BluetoothDevice bluetoothDevice, ConnectionConfig config) {
        this.bluetoothDevice = bluetoothDevice;
        this.config = config;
        connHandler = new ConnHandler(this);
        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();
    }

    public void clearRequestQueue() {
        synchronized (this) {
            requestQueue.clear();
            currentRequest = null;
        }
    }

    /**
     * 设置设备上报数据回调
     */
    public void setCharacteristicChangedCallback(CharacteristicChangedCallback characteristicChangedCallback) {
        this.characteristicChangedCallback = characteristicChangedCallback;
    }

    public void clearRequestQueueByType(Request.RequestType type) {
        synchronized (this) {
            for (Iterator<Request> it = requestQueue.iterator(); it.hasNext(); ) {
                Request request = it.next();
                if (request.type == type) {
                    it.remove();
                }
            }
            if (currentRequest != null && currentRequest.type == type) {
                currentRequest = null;
            }
        }
    }

    void clearRequestQueueAndNotify() {
        synchronized (this) {
            for (Request request : requestQueue) {
                handleFaildCallback(request, REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED, false);
            }
            if (currentRequest != null) {
                handleFaildCallback(currentRequest, REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED, false);
            }
        }
        clearRequestQueue();
    }

    public void release() {
        isReleased = true;
        connHandler.removeCallbacksAndMessages(null);
        clearRequestQueueAndNotify();
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    /**
     * 获取当前连接的配置
     */
    public ConnectionConfig getConfig() {
        return config;
    }

    /**
     * 获取蓝牙服务列表
     */
    public List<BluetoothGattService> getGattServices() {
        if (bluetoothGatt != null) {
            return bluetoothGatt.getServices();
        }
        return new ArrayList<>();
    }
    
    public BluetoothGattService getService(UUID serviceUuid) {
        if (bluetoothGatt != null) {
            return bluetoothGatt.getService(serviceUuid);
        }
        return null;
    }
    
    public BluetoothGattCharacteristic getCharacteristic(UUID serviceUuid, UUID characteristicUuid) {
        if (bluetoothGatt != null) {
            BluetoothGattService service = bluetoothGatt.getService(serviceUuid);
            if (service != null) {
                return service.getCharacteristic(characteristicUuid);
            }
        }
        return null;
    }
    
    public BluetoothGattDescriptor getDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid) {
        if (bluetoothGatt != null) {
            BluetoothGattService service = bluetoothGatt.getService(serviceUuid);
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
                if (characteristic != null) {
                    return characteristic.getDescriptor(descriptorUuid);
                }
            }
        }
        return null;
    }

    /*
     * Clears the internal cache and forces a refresh of the services from the
     * remote device.
     */
    public static boolean refresh(BluetoothGatt bluetoothGatt) {
        try {
            Method localMethod = bluetoothGatt.getClass().getMethod("refresh");
            return (Boolean) localMethod.invoke(bluetoothGatt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
        // 读取到值
        if (currentRequest != null) {
            if (currentRequest.type == Request.RequestType.READ_CHARACTERISTIC) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (currentRequest.callback != null) {
                        handleRequestCallback(currentRequest.callback, Events.newCharacteristicRead(device, currentRequest.requestId, 
                                new GattCharacteristic(characteristic.getService().getUuid(), characteristic.getUuid(), characteristic.getValue())));
                    } else {
                        onCharacteristicRead(currentRequest.requestId, characteristic);
                    }
                } else {
                    handleGattStatusFailed();
                }
                executeNextRequest();
            }
        }
    }

    @Override
    public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
        if (currentRequest != null && currentRequest.waitWriteResult && currentRequest.type == Request.RequestType.WRITE_CHARACTERISTIC) {           
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (currentRequest.remainQueue == null || currentRequest.remainQueue.isEmpty()) {
                    GattCharacteristic charac = new GattCharacteristic(characteristic.getService().getUuid(), characteristic.getUuid(), currentRequest.value);
                    if (currentRequest.callback != null) {
                        handleRequestCallback(currentRequest.callback, Events.newCharacteristicWrite(device, currentRequest.requestId, charac));
                    } else {
                        onCharacteristicWrite(currentRequest.requestId, charac);
                    }
                    executeNextRequest();
                } else {
                    connHandler.removeMessages(MSG_REQUEST_TIMEOUT);
                    connHandler.sendMessageDelayed(Message.obtain(connHandler, MSG_REQUEST_TIMEOUT, currentRequest), config.requestTimeoutMillis);
                    try {
                        java.lang.Thread.sleep(currentRequest.writeDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    currentRequest.sendingBytes = currentRequest.remainQueue.remove();
                    if (writeFail(characteristic, currentRequest.sendingBytes)) {
                        handleWriteFailed(currentRequest);
                    }
                }
            } else {
                handleFaildCallback(currentRequest, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, true);
            }
        }
    }

    @Override
    public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        // 收到设备notify值 （设备上报值）
        onCharacteristicChanged(characteristic);
        if (characteristicChangedCallback != null) {
            try {
                Method method = characteristicChangedCallback.getClass().getMethod("onCharacteristicChanged", BluetoothGattCharacteristic.class);
                execute(method, new Runnable() {
                    @Override
                    public void run() {
                        characteristicChangedCallback.onCharacteristicChanged(characteristic);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReadRemoteRssi(final BluetoothGatt gatt, final int rssi, final int status) {
        if (currentRequest != null) {
            if (currentRequest.type == Request.RequestType.READ_RSSI) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (currentRequest.callback != null) {
                        handleRequestCallback(currentRequest.callback, Events.newRemoteRssiRead(device, currentRequest.requestId, rssi));
                    } else {
                        onReadRemoteRssi(currentRequest.requestId, rssi);
                    }                    
                } else {
                    handleGattStatusFailed();
                }
                executeNextRequest();
            }
        }
    }

    @Override
    public void onDescriptorRead(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
        if (currentRequest != null) {
            BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
            if (currentRequest.type == Request.RequestType.TOGGLE_NOTIFICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleGattStatusFailed();
                }
                if (characteristic.getService().getUuid().equals(pendingCharacteristic.getService().getUuid()) && characteristic.getUuid().equals(pendingCharacteristic.getUuid())) {
                    if (enableNotificationOrIndicationFail(Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, currentRequest.value), true, characteristic)) {
                        handleGattStatusFailed();
                    }
                }
            } else if (currentRequest.type == Request.RequestType.TOGGLE_INDICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleGattStatusFailed();
                }
                if (characteristic.getService().getUuid().equals(pendingCharacteristic.getService().getUuid()) && characteristic.getUuid().equals(pendingCharacteristic.getUuid())) {
                    if (enableNotificationOrIndicationFail(Arrays.equals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE, currentRequest.value), false, characteristic)) {
                        handleGattStatusFailed();
                    }
                }
            } else if (currentRequest.type == Request.RequestType.READ_DESCRIPTOR) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (currentRequest.callback != null) {
                        handleRequestCallback(currentRequest.callback, Events.newDescriptorRead(device, currentRequest.requestId, 
                                new GattDescriptor(characteristic.getService().getUuid(), characteristic.getUuid(), descriptor.getUuid(), descriptor.getValue())));
                    } else {
                        onDescriptorRead(currentRequest.requestId, descriptor);
                    }
                } else {
                    handleGattStatusFailed();
                }
                executeNextRequest();
            }
        }
    }

    @Override
    public void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
        if (currentRequest != null) {
            if (currentRequest.type == Request.RequestType.TOGGLE_NOTIFICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleGattStatusFailed();
                } else {
                    boolean isEnabled = Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, currentRequest.value);
                    if (currentRequest.callback != null) {
                        BluetoothGattCharacteristic ch = descriptor.getCharacteristic();
                        handleRequestCallback(currentRequest.callback, Events.newNotificationChanged(device, currentRequest.requestId, 
                                new GattDescriptor(ch.getService().getUuid(), ch.getUuid(), descriptor.getUuid(), descriptor.getValue()), isEnabled));
                    } else {
                        onNotificationChanged(currentRequest.requestId, descriptor, isEnabled);
                    }
                }
                executeNextRequest();
            } else if (currentRequest.type == Request.RequestType.TOGGLE_INDICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleGattStatusFailed();
                } else {
                    boolean isEnabled = Arrays.equals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE, currentRequest.value);
                    if (currentRequest.callback != null) {
                        BluetoothGattCharacteristic ch = descriptor.getCharacteristic();
                        handleRequestCallback(currentRequest.callback, Events.newIndicationChanged(device, currentRequest.requestId,
                                new GattDescriptor(ch.getService().getUuid(), ch.getUuid(), descriptor.getUuid(), descriptor.getValue()), isEnabled));
                    } else {
                        onIndicationChanged(currentRequest.requestId, descriptor, isEnabled);
                    }
                }
                executeNextRequest();
            }
        }
    }

    @Override
    public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
        if (currentRequest != null) {
            if (currentRequest.type == Request.RequestType.CHANGE_MTU) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (currentRequest.callback != null) {
                        handleRequestCallback(currentRequest.callback, Events.newMtuChanged(device, currentRequest.requestId, mtu));
                    } else {
                        onMtuChanged(currentRequest.requestId, mtu);
                    }
                } else {
                    handleGattStatusFailed();
                }
                executeNextRequest();
            }
        }
    }

    //需要保证currentRequest不为空
    private void handleGattStatusFailed() {
        handleFaildCallback(currentRequest, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, false);
    }
    
    private void handleFaildCallback(String requestId, Request.RequestType requestType, int failType, byte[] value, boolean executeNext) {
        onRequestFialed(requestId, requestType, failType, value);
        if (executeNext) {
            executeNextRequest();
        }
    }
    
    private void handleFaildCallback(Request request, int failType, boolean executeNext) {
        if (request.callback != null) {
            handleRequestCallback(request.callback, Events.newRequestFailed(device, request.requestId, request.type, failType, request.value));
        } else {
            onRequestFialed(request.requestId, request.type, failType, request.value);
        }
        if (executeNext) {
            executeNextRequest();
        }
    }

    @SuppressWarnings("unchecked")
    private void handleRequestCallback(final RequestCallback callback, final Object param) {
        Method method = null;
        try {
              if (param instanceof Events.RequestFailed) {
                  method = callback.getClass().getMethod("onFail", param.getClass());
              } else {
                  method = callback.getClass().getMethod("onSuccess", param.getClass());
              }
        } catch (Exception e) {
            e.printStackTrace();
        }
        execute(method, new Runnable() {
            @Override
            public void run() {
                if (param instanceof Events.RequestFailed) {
                    callback.onFail((Events.RequestFailed) param);
                } else {
                    callback.onSuccess(param);
                }
            }
        });
    }
    
    //根据方法上是否有相应的注解，决定回调线程
    void execute(Method method, Runnable runnable) {
        if (method != null && runnable != null) {
            InvokeThread invokeThread = method.getAnnotation(InvokeThread.class);
            if (invokeThread == null || invokeThread.value() == RunOn.POSTING) {
                runnable.run();
            } else if (invokeThread.value() == RunOn.BACKGROUND) {
                executorService.execute(runnable);
            } else {
                mainHandler.post(runnable);
            }
        }
    }

    /**
     * 改变最大传输单元
     * @param requestId 请求码
     * @param mtu 最大传输单元
     */
    public void changeMtu(@NonNull String requestId, int mtu) {
        changeMtu(requestId, mtu, null);
    }

    /**
     * 改变最大传输单元
     * @param requestId 请求码
     * @param mtu 最大传输单元
     */
    public void changeMtu(@NonNull String requestId, int mtu, RequestCallback<Events.MtuChanged> callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            enqueue(Request.newChangeMtuRequest(requestId, mtu, callback));
        } else if (callback != null) {
            callback.onFail(Events.newRequestFailed(device, requestId, Request.RequestType.CHANGE_MTU, REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW, BleUtils.numberToBytes(false, mtu, 2)));
        } else {
            handleFaildCallback(requestId, Request.RequestType.CHANGE_MTU, REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW, BleUtils.numberToBytes(false, mtu, 2), false);
        }
    }

    /*
     * 请求读取characteristic的值
     * @param requestId 请求码
     */
    public void readCharacteristic(@NonNull String requestId, UUID service, UUID characteristic) {
        readCharacteristic(requestId, service, characteristic, null);
    }

    /*
     * 请求读取characteristic的值
     * @param requestId 请求码
     */
    public void readCharacteristic(@NonNull String requestId, UUID service, UUID characteristic, RequestCallback<Events.CharacteristicRead> callback) {
        enqueue(Request.newReadCharacteristicRequest(requestId, service, characteristic, callback));
    }

    /**
     * 打开Notifications
     *
     * @param requestId 请求码
     * @param enable    开启还是关闭
     */
    public void toggleNotification(@NonNull String requestId, UUID service, UUID characteristic, boolean enable) {
        toggleNotification(requestId, service, characteristic, enable, null);
    }

    /**
     * 打开Notifications
     *
     * @param requestId 请求码
     * @param enable    开启还是关闭
     */
    public void toggleNotification(@NonNull String requestId, UUID service, UUID characteristic, boolean enable, RequestCallback<Events.NotificationChanged> callback) {
        enqueue(Request.newToggleNotificationRequest(requestId, service, characteristic, enable, callback));
    }

    /**
     * @param enable 开启还是关闭
     */
    public void toggleIndication(@NonNull String requestId, UUID service, UUID characteristic, boolean enable) {
        toggleIndication(requestId, service, characteristic, enable, null);
    }

    /**
     * @param enable 开启还是关闭
     */
    public void toggleIndication(@NonNull String requestId, UUID service, UUID characteristic, boolean enable, RequestCallback<Events.IndicationChanged> callback) {
        enqueue(Request.newToggleIndicationRequest(requestId, service, characteristic, enable, callback));
    }

    public void readDescriptor(@NonNull String requestId, UUID service, UUID characteristic, UUID descriptor) {
        readDescriptor(requestId, service, characteristic, descriptor, null);
    }

    public void readDescriptor(@NonNull String requestId, UUID service, UUID characteristic, UUID descriptor, RequestCallback<Events.DescriptorRead> callback) {
        enqueue(Request.newReadDescriptorRequest(requestId, service, characteristic, descriptor, callback));
    }

    public void writeCharacteristic(@NonNull String requestId, UUID service, UUID characteristic, byte[] value) {
        writeCharacteristic(requestId, service, characteristic, value, null);
    }

    public void writeCharacteristic(@NonNull String requestId, UUID service, UUID characteristic, byte[] value, RequestCallback<Events.CharacteristicWrite> callback) {
        if (value == null || value.length == 0) {
            if (callback != null) {
                callback.onFail(Events.newRequestFailed(device, requestId, Request.RequestType.WRITE_CHARACTERISTIC, REQUEST_FAIL_TYPE_VALUE_IS_NULL_OR_EMPTY, value));
            } else {
                handleFaildCallback(requestId, Request.RequestType.WRITE_CHARACTERISTIC, REQUEST_FAIL_TYPE_VALUE_IS_NULL_OR_EMPTY, value, false);
            }
            return;
        }
        enqueue(Request.newWriteCharacteristicRequest(requestId, service, characteristic, value, callback));
    }

    public void readRssi(@NonNull String requestId) {
        readRssi(requestId, null);
    }

    public void readRssi(@NonNull String requestId, RequestCallback<Events.RemoteRssiRead> callback) {
        enqueue(Request.newReadRssiRequest(requestId, callback));
    }

    private void enqueue(Request request) {
        if (isReleased) {
            handleFaildCallback(request, REQUEST_FAIL_TYPE_CONNECTION_RELEASED, false);
        } else {
            synchronized (this) {
                if (currentRequest == null) {
                    executeRequest(request);
                } else {
                    requestQueue.add(request);
                }
            }
        }
    }

    private void executeNextRequest() {
        synchronized (this) {
            connHandler.removeMessages(MSG_REQUEST_TIMEOUT);
            if (requestQueue.isEmpty()) {
                currentRequest = null;
            } else {
                executeRequest(requestQueue.remove(0));
            }
        }
    }

    private static class ConnHandler extends Handler {
        private WeakReference<BaseConnection> weakRef;

        ConnHandler(BaseConnection connection) {
            super(Looper.getMainLooper());
            weakRef = new WeakReference<>(connection);
        }

        @Override
        public void handleMessage(Message msg) {
            BaseConnection connection = weakRef.get();
            if (connection != null) {
                switch (msg.what) {
                    case MSG_REQUEST_TIMEOUT:
                        Request request = (Request) msg.obj;
                        if (connection.currentRequest != null && connection.currentRequest == request) {
                            connection.handleFaildCallback(request, REQUEST_FAIL_TYPE_REQUEST_TIMEOUT, false);
                            connection.executeNextRequest();
                        }
                        break;
                }
                connection.handleMsg(msg);
            }
        }
    }
    
    protected abstract void handleMsg(Message msg);

    private void executeRequest(Request request) {
        currentRequest = request;
        connHandler.sendMessageDelayed(Message.obtain(connHandler, MSG_REQUEST_TIMEOUT, request), config.requestTimeoutMillis);
        if (bluetoothAdapter.isEnabled()) {
            if (bluetoothGatt != null) {
                switch (request.type) {
                    case READ_RSSI:
                        executeReadRssi(request);
                        break;
                    case CHANGE_MTU:
                        executeChangeMtu(request);
                        break;
                    default:
                        BluetoothGattService gattService = bluetoothGatt.getService(request.service);
                        if (gattService != null) {
                            BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(request.characteristic);
                            if (characteristic != null) {
                                switch (request.type) {
                                    case TOGGLE_NOTIFICATION:
                                    case TOGGLE_INDICATION:
                                        executeIndicationOrNotification(characteristic, request);
                                        break;
                                    case READ_CHARACTERISTIC:
                                        executeReadCharacteristic(characteristic, request);
                                        break;
                                    case READ_DESCRIPTOR:
                                        executeReadDescriptor(characteristic, request);
                                        break;
                                    case WRITE_CHARACTERISTIC:
                                        executeWriteCharacteristic(characteristic, request);
                                        break;
                                }
                            } else {
                                handleFaildCallback(request, REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC, true);
                            }
                        } else {
                            handleFaildCallback(request, REQUEST_FAIL_TYPE_NULL_SERVICE, true);
                        }
                        break;
                }
            } else {
                handleFaildCallback(request, REQUEST_FAIL_TYPE_GATT_IS_NULL, true);
            }
        } else {
            handleFaildCallback(request, REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED, true);
        }
    }

    private void executeChangeMtu(Request request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!bluetoothGatt.requestMtu((int) BleUtils.bytesToLong(false, request.value))) {
                handleFaildCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
            }
        } else {
            handleFaildCallback(request, REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW, true);
        }
    }

    private void executeReadRssi(Request request) {
        if (!bluetoothGatt.readRemoteRssi()) {
            handleFaildCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
        }
    }

    private void executeReadCharacteristic(BluetoothGattCharacteristic characteristic, Request request) {
        if (!bluetoothGatt.readCharacteristic(characteristic)) {
            handleFaildCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
        }
    }

    private void executeWriteCharacteristic(BluetoothGattCharacteristic characteristic, Request request) {
        try {
            request.waitWriteResult = config.waitWriteResult;
            request.writeDelay = config.packageWriteDelayMillis;
            int packSize = config.packageSize;
            int requestWriteDelayMillis = config.requestWriteDelayMillis;
            java.lang.Thread.sleep(requestWriteDelayMillis > 0 ? requestWriteDelayMillis : request.writeDelay);
            if (request.value.length > packSize) {
                List<byte[]> list = BleUtils.splitPackage(request.value, packSize);
                if (!request.waitWriteResult) {//不等待则遍历发送
                    for (int i = 0; i < list.size(); i++) {
                        byte[] bytes = list.get(i);
                        if (i > 0) {
                            java.lang.Thread.sleep(request.writeDelay);
                        }
                        if (writeFail(characteristic, bytes)) {//写失败
                            handleWriteFailed(request);
                            return;
                        }
                    }
                } else {//等待则只直接发送第一包，剩下的添加到队列等待回调
                    request.remainQueue = new ConcurrentLinkedQueue<>();
                    request.remainQueue.addAll(list);
                    request.sendingBytes = request.remainQueue.remove();
                    if (writeFail(characteristic, request.sendingBytes)) {//写失败
                        handleWriteFailed(request);
                        return;
                    }
                }
            } else {
                request.sendingBytes = request.value;
                if (writeFail(characteristic, request.value)) {
                    handleWriteFailed(request);
                    return;
                }
            }
            if (!request.waitWriteResult) {
                if (request.callback != null) {
                    handleRequestCallback(request.callback, Events.newCharacteristicWrite(device, request.requestId, new GattCharacteristic(characteristic.getService().getUuid(), characteristic.getUuid(), request.value)));
                } else {
                    onCharacteristicWrite(request.requestId, new GattCharacteristic(characteristic.getService().getUuid(), characteristic.getUuid(), request.value));
                }
                executeNextRequest();
            }
        } catch (Exception e) {
            handleWriteFailed(request);
        }
    }

    private void handleWriteFailed(Request request) {
        connHandler.removeMessages(MSG_REQUEST_TIMEOUT);
        request.remainQueue = null;
        handleFaildCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
    }

    private boolean writeFail(BluetoothGattCharacteristic characteristic, byte[] value) {
        characteristic.setValue(value);
        Integer writeType = config.getWriteType(characteristic.getService().getUuid(), characteristic.getUuid());
        if (writeType != null && (writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT || writeType == BluetoothGattCharacteristic.WRITE_TYPE_SIGNED ||
                writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)) {
            characteristic.setWriteType(writeType);
        }
        return !bluetoothGatt.writeCharacteristic(characteristic);
    }

    private void executeReadDescriptor(BluetoothGattCharacteristic characteristic, Request request) {
        BluetoothGattDescriptor gattDescriptor = characteristic.getDescriptor(request.descriptor);
        if (gattDescriptor != null) {
            if (!bluetoothGatt.readDescriptor(gattDescriptor)) {
                handleFaildCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
            }
        } else {
            handleFaildCallback(request, REQUEST_FAIL_TYPE_NULL_DESCRIPTOR, true);
        }
    }

    private void executeIndicationOrNotification(BluetoothGattCharacteristic characteristic, Request request) {
        pendingCharacteristic = characteristic;
        BluetoothGattDescriptor gattDescriptor = pendingCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (gattDescriptor == null || !bluetoothGatt.readDescriptor(gattDescriptor)) {
            handleFaildCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
        }
    }

    private boolean enableNotificationOrIndicationFail(boolean enable, boolean notification, BluetoothGattCharacteristic characteristic) {
        //setCharacteristicNotification是设置本机
        if (!bluetoothAdapter.isEnabled() || bluetoothGatt == null || !bluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            return true;
        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (descriptor == null) {
            return true;
        }
        if (enable) {
            descriptor.setValue(notification ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        //部分蓝牙在Android6.0及以下需要设置写入类型为有响应的，否则会enable回调是成功，但是仍然无法收到notification数据
        int writeType = characteristic.getWriteType();
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        boolean result = bluetoothGatt.writeDescriptor(descriptor);//把设置写入蓝牙设备
        characteristic.setWriteType(writeType);
        return !result;
    }
}