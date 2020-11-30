package com.hongxing.hxs.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

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

    @SuppressLint({"MissingPermission", "HardwareIds"})
    public static String getDeviceID(@Nullable Context context){
        if (context==null)return null;
        String id=null;
        TelephonyManager mt = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            if (mt != null) {
                id= UUID.nameUUIDFromBytes(mt.getMeid().getBytes()).toString();
//                return mt.getMeid();
            }
        }else {
            if (mt != null) {
                id= UUID.nameUUIDFromBytes(mt.getDeviceId().getBytes()).toString();
//                return mt.getDeviceId();
            }
        }
        return id;
    }
}
