package cn.zfs.bledemo;


import android.app.Application;
import android.util.Log;

import com.snail.commons.AppHolder;
import com.snail.easyble.core.Ble;
import com.snail.easyble.core.BleLogger;


/**
 * 
 * date: 2018/5/4 18:03
 * author: zengfansheng
 */
public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppHolder.init(this);
        Ble.Companion.getInstance().initialize(this);
        Ble.Companion.getInstance().setLogPrintLevel(BleLogger.ALL);//输出日志
        Ble.Companion.println(MyApp.class, Log.DEBUG, "initialize");
    }
}
