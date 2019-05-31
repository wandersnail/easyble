package cn.zfs.bledemo;

import android.app.Application;
import android.util.Log;
import com.snail.commons.AppHolder;
import com.snail.easyble.core.Ble;
import com.snail.easyble.util.BleLogger;
import org.jetbrains.annotations.NotNull;

/**
 * date: 2019/5/31 16:48
 * author: zengfansheng
 */
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppHolder.init(this);
        Ble.Companion.getInstance().initialize(this);
        Ble.Companion.getInstance().getLogger().setLogEnabled(true);
        Ble.Companion.getInstance().getLogger().setLogCallback(new BleLogger.Callback() {
            @Override
            public void onLog(@NotNull String tag, int type, int priority, @NotNull String log) {
                switch(priority) {
                    case Log.VERBOSE:		
                		break;
                    case Log.INFO:		
                		break;
                    case Log.DEBUG:
                        break;
                    case Log.WARN:
                        break;
                    case Log.ERROR:
                        break;
                }
            }
        });
    }
}
