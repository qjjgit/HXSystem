package com.hongxing.hxs.utils;

import android.content.Context;
import android.os.Environment;

import androidx.annotation.NonNull;

public class CommonUtils {

    public static String getAPPStoragePath(@NonNull Context context){
        return context.getExternalFilesDir(null).getPath();
    }

    /**
     * 获取cache路径
     * @param context context
     * @return string
     */
    public static String getDiskCachePath(@NonNull Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            return context.getExternalCacheDir().getPath();
        } else {
            return context.getCacheDir().getPath();
        }
    }
}
