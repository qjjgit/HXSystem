package com.hongxing.hxs.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Message;
import android.view.Gravity;
import android.widget.Toast;

import com.hongxing.hxs.MainActivity;

import android.os.Handler;

import androidx.annotation.NonNull;

public class ToastUtil {
    @SuppressLint("HandlerLeak")
    private static Handler handler=new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0x00:if (toast!=null)toast.show();break;
            }
        }
    };
    private static Toast toast;//实现不管我们触发多少次Toast调用，都只会持续一次Toast显示的时长

    /**
     * 短时间显示Toast【居下】
     * @param msg 显示的内容-字符串*/
    @SuppressLint("ShowToast")
    public static void showShortToast(String msg) {
        Context context=MainActivity.getMainContext();
        if(context != null){
            short i=0;
            if (toast!=null){i=1;toast.cancel();}
            toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
            //1、setGravity方法必须放到这里，否则会出现toast始终按照第一次显示的位置进行显示（比如第一次是在底部显示，那么即使设置setGravity在中间，也不管用）
            //2、虽然默认是在底部显示，但是，因为这个工具类实现了中间显示，所以需要还原，还原方式如下：
            toast.setGravity(Gravity.BOTTOM, 0, dip2px(context));
            if (i==1)
            handler.sendEmptyMessageDelayed(0x00,200);
            else toast.show();
        }
    }
    /**
     * 短时间显示Toast【居中】
     * @param msg 显示的内容-字符串*/
    @SuppressLint("ShowToast")
    public static void showShortToastCenter(String msg){
        Context context = MainActivity.getMainContext();
        if(context != null) {
            if (toast == null) {
                toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
            } else {
                toast.setText(msg);
            }
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    /**
     * 短时间显示Toast【居上】
     * @param msg 显示的内容-字符串*/
    @SuppressLint("ShowToast")
    public static void showShortToastTop(String msg){
        Context context = MainActivity.getMainContext();
        if(context != null) {
            if (toast == null) {
                toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
            } else {
                toast.setText(msg);
            }
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
        }
    }

    /**
     * 长时间显示Toast【居下】
     * @param msg 显示的内容-字符串*/
    @SuppressLint("ShowToast")
    public static void showLongToast(String msg) {
        if (toast!=null)toast.cancel();
        Context context = MainActivity.getMainContext();
        if(context != null) {
            if (toast == null) {
                toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
            } else {
                toast.setText(msg);
            }
            toast.setGravity(Gravity.BOTTOM, 0, dip2px(context));
            toast.show();
        }
    }
    /**
     * 长时间显示Toast【居中】
     * @param msg 显示的内容-字符串*/
    @SuppressLint("ShowToast")
    public static void showLongToastCenter(String msg){
        Context context = MainActivity.getMainContext();
        if(context != null) {
            if (toast == null) {
                toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
            } else {
                toast.setText(msg);
            }
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }
    /**
     * 长时间显示Toast【居上】
     * @param msg 显示的内容-字符串*/
    @SuppressLint("ShowToast")
    public static void showLongToastTop(String msg){
        Context context = MainActivity.getMainContext();
        if(context != null) {
            if (toast == null) {
                toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
            } else {
                toast.setText(msg);
            }
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
        }
    }
    private static int dip2px(Context context) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) ((float) 64 * scale + 0.5f);
    }
}