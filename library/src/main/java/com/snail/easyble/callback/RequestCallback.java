package com.snail.easyble.callback;

import android.support.annotation.NonNull;

import com.snail.easyble.annotation.InvokeThread;
import com.snail.easyble.event.Events;

/**
 * 描述: 请求回调
 * 时间: 2018/12/2 10:32
 * 作者: zengfansheng
 */
public interface RequestCallback<T> {
    @InvokeThread
    void onSuccess(@NonNull T data);

    @InvokeThread
    void onFail(@NonNull Events.RequestFailed requestFailed);
}
