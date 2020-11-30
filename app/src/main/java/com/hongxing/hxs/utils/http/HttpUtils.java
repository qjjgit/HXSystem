package com.hongxing.hxs.utils.http;

import android.content.Context;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.hongxing.hxs.MainActivity;
import com.hongxing.hxs.utils.CommonUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class HttpUtils {
    final static private String DOMAIN_NAME="http://3579h68942.oicp.vip ";

    public static void doGet(@NonNull String url,Map<String,String> parameters,@NonNull Listener listener){
        new HttpThread(url,listener,HttpThread.GET,parameters).start();
    }
    public static void doPost(@NonNull String url,@NonNull Map<String,String> form,@NonNull Listener listener){
        new HttpThread(url,listener,HttpThread.POST,form).start();
    }

    public static void uploadFile(@NonNull String url,@NonNull String filePath,@NonNull Map<String,String> form,@NonNull Listener listener){
        CheckReady checkReady = new CheckReady();
        HttpThread thread = new HttpThread(url, new Listener() {
            @Override
            public void success(String response) {
                checkReady.setOk(true);
            }
            @Override
            public void error(String error_msg) {
                checkReady.setOk(false);System.out.println(error_msg);
            }
        }, HttpThread.POST, form);
        thread.start();int i=0;
        while (!checkReady.isOk()) {
        }
        new HttpThread(url,listener,filePath).start();
    }

    public static void downloadFile(@NonNull DownloadListener listener){
        CheckReady checkReady = new CheckReady();
        HttpThread thread = new HttpThread(DOMAIN_NAME+"/getBackupFileSize", new Listener() {
            @Override
            public void success(String response) {
                checkReady.setOk(true);
                checkReady.setResponse(response);
            }
            @Override
            public void error(String error_msg) {
                checkReady.setOk(false);System.out.println(error_msg);
            }
        });
        thread.start();int i=0;
        while (!checkReady.isOk()) { }
        System.out.println("data length: "+checkReady.getResponse());
//        final Context context = MainActivity.getMainContext();
        Map<String, String> map = new HashMap<String, String>() {{
            put("deviceID", "admin");
        }};
        String path=Environment.getExternalStorageDirectory().getPath()
                + File.separator + "鸿兴系统"+File.separator+"temp.zip";
        new HttpThread(DOMAIN_NAME+"/download",listener,map,path,Long.valueOf(checkReady.getResponse())).start();
    }

    public interface Listener{
        void success(String response);
        void error(String error_msg);
    }
    public interface DownloadListener extends Listener{
        void startDownload();
        void progress(int progress);
        void success(File file);
    }
    private static class CheckReady{
        private boolean ok=false;
        private String response;
        boolean isOk() {
            return ok;
        }
        void setOk(boolean ok) {
            this.ok = ok;
        }
        public String getResponse() {
            return response;
        }
        public void setResponse(String response) {
            this.response = response;
        }
    }
}
