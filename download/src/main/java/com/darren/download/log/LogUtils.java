package com.darren.download.log;

import android.util.Log;

import com.example.download.BuildConfig;

public class LogUtils {
    private static final boolean isDebug = BuildConfig.DEBUG;
    public static void logd(String tag, String message) {
        if (isDebug) {
            Log.d("darren", tag + " " + message);
        }
    }
}
