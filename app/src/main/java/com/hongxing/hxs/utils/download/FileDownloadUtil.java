package com.hongxing.hxs.utils.download;

import androidx.annotation.NonNull;

import com.hongxing.hxs.utils.CommonUtils;

public class FileDownloadUtil {
    // 记录读取了多少，一共读取了多少
    static long process;
    // 记录文件总大小
    static long sum;

    public interface Listener{
        void startDownload();
        void progress(double progress);
        void now_speed(String speed);
        void success();
        void error(Exception e);
    }

    public static void downloadNewestAPK(Listener listener){
        download(CommonUtils.SERVERADDRESS+"/download/hx_system.apk",null,listener);
    }
    public static void download(@NonNull String url, String filePath, Listener listener){
        //1线程下载
        new DownloadFilePool(url,filePath,1,listener).doDownload();
    }
    public static void multiThreadDownload(@NonNull String url, String filePath, Listener listener){
        //3线程下载
        new DownloadFilePool(url,filePath,3,listener).doDownload();
    }
}
