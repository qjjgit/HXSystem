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

    public static void doGet(Map<String,String> parameters,@NonNull Listener listener){
        new HttpThread(CommonUtils.SERVERADDRESS,listener,HttpThread.GET,parameters).start();
    }
    public static void doPost(@NonNull String url,@NonNull Map<String,String> form,@NonNull Listener listener){
        new HttpThread(url,listener,HttpThread.POST,form).start();
    }

    public static void sendErrorLog(Context context,@NonNull String content){
        Map<String,String> map=new HashMap<>();
        map.put("deviceID",CommonUtils.getDeviceID(context));map.put("content",content);
        new HttpThread(CommonUtils.SERVERADDRESS + "/sendErrorLog", new Listener() {
            @Override
            public void startFileTransfer() { }
            @Override
            public void success(String response) {System.out.println("send error log ok");}
            @Override
            public void progress(int progress) { }
            @Override
            public void error(Exception e) {System.out.println("send error log failed");}
        }, HttpThread.POST, map).start();
    }
    public static void sendErrorLog(Context context,@NonNull String content,@NonNull Listener listener){
        Map<String,String> map=new HashMap<>();
        map.put("deviceID",CommonUtils.getDeviceID(context));map.put("content",content);
        new HttpThread(CommonUtils.SERVERADDRESS+"/sendErrorLog",listener,HttpThread.POST,map).start();
    }

    public static void uploadFile(@NonNull Map<String,String> form,@NonNull Listener listener){
        String url = CommonUtils.SERVERADDRESS + "/upload";
        CheckStatus status = new CheckStatus();
        HttpThread thread = new HttpThread(url, new Listener() {
            @Override
            public void startFileTransfer() { }
            @Override
            public void success(String response) {
                status.setOk(true);
            }
            @Override
            public void progress(int progress) { }
            @Override
            public void error(Exception e) {
                status.setError(true);listener.error(e);
            }
        }, HttpThread.POST, form);
        thread.start();
        while (!status.isOk()&&!status.isError()) { }
        if (status.isError()) return;
        String file=CommonUtils.getBackupPath()+File.separator+"upload_temp.zip";
        new HttpThread(url,listener,file).start();
    }

    public static void uploadImg(@NonNull Map<String,String> form,@NonNull File img,@NonNull Listener listener){
        String url = CommonUtils.SERVERADDRESS + "/uploadImg";
        CheckStatus status = new CheckStatus();
        HttpThread thread = new HttpThread(url, new Listener() {
            @Override
            public void startFileTransfer() { }
            @Override
            public void success(String response) {
                status.setOk(true);
            }
            @Override
            public void progress(int progress) { }
            @Override
            public void error(Exception e) {
                status.setError(true);listener.error(e);
            }
        }, HttpThread.POST, form);
        thread.start();
        while (!status.isOk()&&!status.isError()) { }
        if (status.isError()) return;
        new HttpThread(url,listener,img.getPath(),HttpThread.UPLOADIMG).start();
    }

    public static void downloadFile(@NonNull DownloadListener listener){
        final Context context = MainActivity.getMainContext();
        Map<String, String> map = new HashMap<String, String>() {{
            put("deviceID", CommonUtils.getDeviceID(context));
        }};
        CheckStatus status = new CheckStatus();
        HttpThread thread = new HttpThread(CommonUtils.SERVERADDRESS+"/getBackupFileSize",new Listener(){
            @Override
            public void startFileTransfer() { }
            @Override
            public void success(String response) {
                status.setOk(true);
                status.setResponse(response);
            }
            @Override
            public void progress(int progress) { }
            @Override
            public void error(Exception e) {
                status.setError(true);System.out.println(e);
            }
        },HttpThread.GET,map);
        thread.start();
        while (!status.isOk()&&!status.isError()) { }
        if (status.isError()) return;
        String path=Environment.getExternalStorageDirectory().getPath()
                + File.separator + "鸿兴系统"+File.separator+"temp.zip";
        new HttpThread(CommonUtils.SERVERADDRESS+"/download",
                listener,map,path,Long.valueOf(status.getResponse())).start();
    }

    public interface Listener{
        void startFileTransfer();
        void success(String response);
        void progress(int progress);
        void error(Exception e);
    }
    public interface DownloadListener extends Listener{
        void success(File file);
    }
    private static class CheckStatus{
        private boolean ok=false;
        private boolean error=false;
        private String response;
        boolean isOk() {
            return ok;
        }
        void setOk(boolean ok) {
            this.ok = ok;
        }
        boolean isError() {
            return error;
        }
        void setError(boolean error) {
            this.error = error;
        }
        String getResponse() {
            return response;
        }
        void setResponse(String response) {
            this.response = response;
        }
    }
}
