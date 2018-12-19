package com.snail.easyble.callback

import com.snail.easyble.annotation.InvokeThread
import com.snail.easyble.event.Events

/**
 * 描述: 请求回调
 * 时间: 2018/12/2 10:32
 * 作者: zengfansheng
 */
interface RequestCallback<T> {
    /**
     * 请求成功
     * @param data 成功时带的数据
     */
    @InvokeThread
    fun onSuccess(data: T)

    @InvokeThread
    fun onFail(requestFailed: Events.RequestFailed)
}
