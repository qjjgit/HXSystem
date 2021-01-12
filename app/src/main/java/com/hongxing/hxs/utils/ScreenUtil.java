package com.hongxing.hxs.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class ScreenUtil {
    /**
     * 获取屏幕相关参数
     *
     * @param context 上下文
     * @return DisplayMetrics 屏幕宽高
     */
    public static DisplayMetrics getScreenSize(Context context){
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        display.getMetrics(metrics);
        return metrics;
    }

    /**
     * 获取屏幕density(密度)
     *
     * @param context 上下文
     * @return density 屏幕 density
     */
    static float getDeviceDensity(Context context){
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.density;
    }
}
