package cn.zfs.bledemo;


import android.app.Application;

import com.zfs.commons.AppHolder;


/**
 * 描述:
 * 时间: 2018/5/4 18:03
 * 作者: zengfansheng
 */
public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppHolder.init(this);
    }
}
