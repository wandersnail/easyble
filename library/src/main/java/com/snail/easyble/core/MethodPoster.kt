package com.snail.easyble.core

import android.os.Handler
import com.snail.easyble.annotation.InvokeThread
import com.snail.easyble.annotation.RunOn
import java.lang.reflect.Method
import java.util.concurrent.ExecutorService

/**
 *
 *
 * date: 2019/1/28 20:47
 * author: zengfansheng
 */
internal class MethodPoster(private val executorService: ExecutorService, private val mainHandler: Handler) {
    //Callback on different threads by annotation
    private fun post(method: Method?, runnable: Runnable) {
        if (method != null) {
            val invokeThread = method.getAnnotation(InvokeThread::class.java)
            if (invokeThread == null || invokeThread.value === RunOn.POSTING) {
                runnable.run()
            } else if (invokeThread.value === RunOn.BACKGROUND) {
                executorService.execute(runnable)
            } else {
                mainHandler.post(runnable)
            }
        }
    }

    fun post(obj: Any, methodName: String, valueTypePairs: Array<ValueTypePair>?) {
        if (valueTypePairs == null || valueTypePairs.isEmpty()) {
            try {
                val method = obj.javaClass.getMethod(methodName)
                post(method, Runnable {
                    try {
                        method.invoke(obj)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                })
            } catch (e: NoSuchMethodException) {}            
        } else {
            val params = arrayOfNulls<Any>(valueTypePairs.size)
            val paramTypes = arrayOfNulls<Class<*>>(valueTypePairs.size)
            valueTypePairs.forEachIndexed { i, vt ->
                params[i] = vt.value
                paramTypes[i] = vt.valueType
            }
            try {
                val method = obj.javaClass.getMethod(methodName, *paramTypes)
                post(method, Runnable {
                    try {
                        method.invoke(obj, *params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                })
            } catch (e: NoSuchMethodException) {}            
        }
    }

    fun post(obj: Any, methodInfo: MethodInfo) {
        post(obj, methodInfo.name, methodInfo.valueTypePairs)
    }
}