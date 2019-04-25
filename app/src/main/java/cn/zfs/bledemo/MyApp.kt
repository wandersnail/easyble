package cn.zfs.bledemo


import android.app.Application
import android.util.Log
import com.snail.commons.AppHolder
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
        Ble.instance.logger.logEnabled = true
        Ble.instance.logger.logCallback = object : BleLogger.Callback {
            override fun onLog(tag: String, type: Int, priority: Int, log: String) {
                when (priority) {
                    Log.VERBOSE -> {}
                    Log.INFO -> {}
                    Log.DEBUG -> {}
                    Log.WARN -> {}
                    Log.ERROR -> {}
                    Log.ASSERT -> {}
                }
            }
        }
    }
}
