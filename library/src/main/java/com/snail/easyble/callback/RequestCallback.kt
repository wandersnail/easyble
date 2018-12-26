package com.snail.easyble.callback

import com.snail.easyble.annotation.InvokeThread
import com.snail.easyble.event.Events

/**
 * date: 2018/12/2 10:32
 * author: zengfansheng
 */
interface RequestCallback<T> {
    @InvokeThread
    fun onSuccess(data: T)

    @InvokeThread
    fun onFail(requestFailed: Events.RequestFailed)
}
