package com.snail.easyble.core;

import android.support.annotation.NonNull;
import android.util.Log;

/**
 * 描述: 蓝牙库相关日志输出
 * 时间: 2018/6/20 12:33
 * 作者: zengfansheng
 */
public class BleLogger {
    public static final int NONE = 1;
    public static final int VERBOSE = NONE << 1;
    public static final int INFO = VERBOSE << 1;
    public static final int DEBUG = INFO << 1;
    public static final int WARN = DEBUG << 1;
    public static final int ERROR = WARN << 1;
    public static final int ALL = VERBOSE|INFO|DEBUG|WARN|ERROR;

    public interface Filter {
        boolean accept(@NonNull String log);
    }

    /**
     * 控制输出级别<br>{@link #NONE}, {@link #VERBOSE}, {@link #DEBUG}, {@link #INFO}, {@link #WARN}, {@link #ERROR}
     */
    private int printLevel = NONE;
    private Filter filter;
    
    public void setPrintLevel(int printLevel) {
        this.printLevel = printLevel;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }
    
    private boolean accept(int priority) {
        int level = getLevel(priority);
        return (printLevel & NONE) != NONE && (printLevel & level) == level;
    }
    
    public static int getLevel(int priority) {
        switch(priority) {
            case Log.ERROR:
                return ERROR;
            case Log.WARN:
                return WARN;
            case Log.INFO:
                return INFO;
            case Log.DEBUG:
                return DEBUG;
            case Log.VERBOSE:
                return VERBOSE;
            default:
                return NONE;
        }
    }

    void println(String tag, int priority, @NonNull String msg) {        
        if (accept(priority) && (filter == null || filter.accept(msg))) {
            Log.println(priority, tag, msg);
        }
    }
}
