package cn.zfs.bledemo;

import com.snail.easyble.core.BleConfig;
import com.snail.easyble.core.EventObservable;

import org.jetbrains.annotations.NotNull;

/**
 * date: 2019-06-29 10:17
 * author: zengfansheng
 */
public class MyConfig extends BleConfig {
    @Override
    public void setEventObservable(@NotNull EventObservable eventObservable) {
        super.setEventObservable(eventObservable);
    }


}
