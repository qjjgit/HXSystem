package com.hongxing.hxs.ui.dashboard;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;

public class OnDoubleClickListener implements View.OnTouchListener{
    private int count = 0;//点击次数
    private long firstClick = 0;//第一次点击时间
    private long secondClick = 0;//第二次点击时间
    private float x=-1,y=-1;
    /**
     * 两次点击时间间隔，单位毫秒
     */
    private final int totalTime = 500;
    /**
     * 自定义回调接口
     */
    private DoubleClickCallback mCallback;

    public interface DoubleClickCallback {
        void onDoubleClick();
    }
    OnDoubleClickListener(DoubleClickCallback callback) {
        super();
        this.mCallback = callback;
    }
    /**
     * 触摸事件处理
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (MotionEvent.ACTION_UP == event.getAction()) {//按下
            count++;
            if (1 == count) {
                x=event.getX();y=event.getY();
                firstClick = System.currentTimeMillis();//记录第一次点击时间
            } else if (2 == count) {
                secondClick = System.currentTimeMillis();//记录第二次点击时间
                float xx=event.getX(),yy=event.getY();
                //判断二次点击时间间隔是否在设定的间隔时间之内
                if (secondClick - firstClick < totalTime &&checkXYRange(xx,yy)) {
                    if (mCallback != null) {
                        mCallback.onDoubleClick();
                    }
                    count = 0;
                    firstClick = 0;
                    x=-1;y=-1;
                } else {
                    firstClick = secondClick;
                    count = 1;
                    x=xx;y=yy;
                }
                secondClick = 0;
            }
        }
        return true;
    }
    //检查双击的坐标是否在 ±20之内
    private boolean checkXYRange(float xx,float yy){
        return (xx>x-20f&&xx<x+20f)&&(yy > y - 20f && yy < y + 20f);
    }
}
