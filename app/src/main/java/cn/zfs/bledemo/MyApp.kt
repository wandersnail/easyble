package cn.zfs.bledemo


import android.app.Application
import android.util.Log
import com.snail.commons.AppHolder
import com.snail.easyble.callback.LogCallback
import com.snail.easyble.core.Ble
import com.snail.easyble.util.BleLogger


/**
 *
 * date: 2018/5/4 18:03
 * author: zengfansheng
 */
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppHolder.init(this)
        Ble.instance.initialize(this)
        BleLogger.logEnabled = true
        BleLogger.logCallback = object : LogCallback {
            override fun onLog(priority: Int, log: String) {
                when (priority) {
                    Log.VERBOSE -> TODO()
                    Log.INFO -> TODO()
                    Log.DEBUG -> TODO()
                    Log.WARN -> TODO()
                    Log.ERROR -> TODO()
                    Log.ASSERT -> TODO()
                }
            }
        }
    }
}
