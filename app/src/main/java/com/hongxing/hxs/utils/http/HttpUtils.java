package com.hongxing.hxs.utils.http;

import androidx.annotation.NonNull;

import com.hongxing.hxs.MainActivity;
import com.hongxing.hxs.db.DBManager;
import com.hongxing.hxs.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.hongxing.hxs.utils.http.HttpThread.DOWNLOAD_otherFile;

public class HttpUtils {
    public static final int fromSELF=0x00;
    public static final int fromADMIN=0x01;

    public static void doGet(Map<String,String> parameters,@NonNull Listener listener){
        new HttpThread(CommonUtils.SERVERADDRESS,listener,HttpThread.GET,parameters).start();
    }
    public static void doPost(@NonNull String url,@NonNull Map<String,String> form,@NonNull Listener listener){
        new HttpThread(url,listener,HttpThread.POST,form).start();
    }

    public static void sendErrorLog(@NonNull String content){
        Map<String,String> map=new HashMap<>();
        map.put("deviceID",CommonUtils.getDeviceID());map.put("content",content);
        new HttpThread(CommonUtils.SERVERADDRESS + "/sendErrorLog", new Listener() {
            @Override
            public void startFileTransfer() { }
            @Override
            public void success(String response) {System.out.println("send error log ok");}
            @Override
            public void error(Exception e) {System.out.println("send error log failed");}
        }, HttpThread.POST, map).start();
    }
    public static void sendErrorLog(@NonNull String content,@NonNull Listener listener){
        Map<String,String> map=new HashMap<>();
        map.put("deviceID",CommonUtils.getDeviceID());map.put("content",content);
        new HttpThread(CommonUtils.SERVERADDRESS+"/sendErrorLog",listener,HttpThread.POST,map).start();
    }

    public static void uploadDBFile(@NonNull Listener listener){
        String url = CommonUtils.SERVERADDRESS + "/upload";
        new HttpThread(url,listener,DBManager.db_file).start();
    }

    public static void uploadImg(@NonNull File img,@NonNull Listener listener){
        String url = CommonUtils.SERVERADDRESS + "/uploadImg";
        new HttpThread(url,listener,img).start();
    }

    public static void downloadFile(int from, @NonNull Listener listener){
        String path=MainActivity.APPStoragePath +"/databases/temp.s3db";
        String url=CommonUtils.SERVERADDRESS +"/getDBFile_fromAdmin";
        if (from==fromSELF)url=CommonUtils.SERVERADDRESS +"/getDBFile_fromSelf";
        new HttpThread(url,
                listener,path).start();
    }

    public static boolean isAdmin(){
        CheckStatus status = new CheckStatus();
        new HttpThread(CommonUtils.SERVERADDRESS+"/checkAdmin", new Listener() {
            @Override
            public void startFileTransfer() { }
            @Override
            public void success(String response) {status.OK();status.setResponse(response);}
            @Override
            public void error(Exception e) { status.ERROR();e.printStackTrace(); }
        },HttpThread.POST,null).start();
        while (!status.isOk()&&!status.isError()){}
        boolean is=false;
        System.out.println(status.getResponse());
        if (status.isOk())is="response:yes".equals(status.getResponse());
        return is;
    }
    public static void checkNewestVersion(Listener listener){
        listener.startFileTransfer();
        new HttpThread(CommonUtils.SERVERADDRESS+"/newestAPPVersion",listener,HttpThread.POST,null).start();
    }

    public static void getADImagesURLListFile2Local(Listener listener){
        String path=CommonUtils.getDiskCachePath() +"/img-url-list.temp";
        String url=CommonUtils.SERVERADDRESS +"/getADImagesURLListFile";
        new HttpThread(url,listener, path, DOWNLOAD_otherFile).start();
    }

    /**
     * @Description: 获取 url 连接
     * @param: @param urlLocation
     * @param: @return HttpURLConnection实例化对象
     * @param: @throws IOException
     */
    public static HttpURLConnection getDoGetConnection(URL url) throws IOException {
        return getHttpConnection(url);
    }
    public static HttpURLConnection getDoGetConnection(String url_str) throws IOException {
        return getHttpConnection(new URL(url_str));
    }
    private static HttpURLConnection getHttpConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.setDoOutput(false);
        connection.setUseCaches(false);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(7000);
        connection.setRequestProperty("deviceID", CommonUtils.getDeviceID());
        return connection;
    }

    public interface Listener{
        void startFileTransfer();
        void success(String response);
        void error(Exception e);
    }
    private static class CheckStatus{
        private boolean ok=false;
        private boolean error=false;
        private String response;
        boolean isOk() {
            return ok;
        }
        void OK(){this.ok=true;}
        boolean isError() {
            return error;
        }
        void ERROR(){this.error=true;}
        String getResponse() {
            return response;
        }
        void setResponse(String response) {
            this.response = response;
        }
    }
}
