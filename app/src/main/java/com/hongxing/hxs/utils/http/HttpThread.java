package com.hongxing.hxs.utils.http;

import com.hongxing.hxs.MainActivity;
import com.hongxing.hxs.utils.CommonUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HttpThread extends Thread{
    final static int GET = 0;
    final static int POST = 1;
    private final static int UPLOAD = 2;
    private final static int DOWNLOAD_dbFile = 3;
    final static int DOWNLOAD_otherFile = 4;
    private String url;
    private HttpUtils.Listener listener;
    private Map<String,String> form;
    private File file;
    private int fileAction=0;
    private int requestMethod;
    private HttpURLConnection connection;
    private BufferedReader bufferedReader;

    HttpThread(String url, HttpUtils.Listener listener, File file) {
        this.url = url;
        this.listener = listener;
        this.requestMethod=POST;
        this.file = file;
        this.fileAction=UPLOAD;
    }
    HttpThread(String url, HttpUtils.Listener listener, String filePath) {
        this.url = url;
        this.listener = listener;
        this.requestMethod=POST;
        this.file = new File(filePath);
        this.fileAction=DOWNLOAD_dbFile;
    }
    HttpThread(String url, HttpUtils.Listener listener, String filePath,int action) {
        this.url = url;
        this.listener = listener;
        this.requestMethod=POST;
        this.file = new File(filePath);
        this.fileAction=action;
    }
    HttpThread(String url, HttpUtils.Listener listener) {
        this.url = url;
        this.listener = listener;
        this.requestMethod=GET;
    }
    HttpThread(String url, HttpUtils.Listener listener, int requestMethod, Map<String, String> form) {
        this.url = url;
        this.listener = listener;
        this.requestMethod=requestMethod;
        this.form = form;
    }

    @Override
    public void run() {
        super.run();
        if (requestMethod==GET)doGet();
        if (requestMethod==POST)doPost();
    }

    private void doGet(){
        try {
            if (form!=null&&!form.isEmpty()){
                url += "?" + formDataConnect();
            }
            connection=(HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.connect();
            int code = connection.getResponseCode();
            if (code ==200) listener.success(getResponse());
            else listener.error(new Exception("responseCode "+code));
        } catch (Exception e) {
            listener.error(e);
        }finally {
            try {
                if (connection!=null)connection.disconnect();
                if (bufferedReader!=null)bufferedReader.close();
            } catch (IOException ignored) { }
        }
    }

    private void initPost() throws IOException {
        connection=(HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("deviceID", CommonUtils.getDeviceID());
        if (fileAction==UPLOAD&&file!=null){
            connection.setRequestProperty("fileName",file.getName());
        }
    }
    private void doPost(){
        try {
            initPost();
            connection.connect();
            OutputStream outputStream = connection.getOutputStream();
            if (form!=null&&!form.isEmpty()){
                PrintWriter writer = new PrintWriter(outputStream);
                writer.write(formDataConnect());writer.flush();writer.close();
            }
            if (file!=null&&fileAction==UPLOAD){
                DataOutputStream dos = new DataOutputStream(outputStream);
                FileInputStream is = new FileInputStream(file);
                byte[] bytes = new byte[512*1024];int len;
                while ((len=is.read(bytes))!=-1){
                    dos.write(bytes,0,len);
                }is.close();dos.flush();dos.close();
            }
            int responseCode = connection.getResponseCode();
            if (responseCode ==200) {
                if (fileAction==DOWNLOAD_dbFile||fileAction==DOWNLOAD_otherFile){
                    listener.startFileTransfer();
                    byte[] bytes = new byte[16 * 1024];int len;
                    BufferedInputStream bin = new BufferedInputStream(connection.getInputStream());
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                    while ((len=bin.read(bytes))!=-1){
                        bos.write(bytes,0,len);
                    }bos.flush();bos.close();bin.close();
                    if (fileAction==DOWNLOAD_dbFile){
                        File db = new File(MainActivity.APPStoragePath + "/databases/hxs.s3db");
                        if (db.delete()) file.renameTo(db);
                        listener.success("同步完成!");
                    }else listener.success("getADImagesURLListFile ok");
                }else listener.success(getResponse());
            }else if(responseCode==205){
                String msg = connection.getHeaderField("msg");
                if ("no backup".equals(msg))msg="您未进行过备份";
                listener.error(new Exception(msg));
            }
            else listener.error(new Exception("responseCode "+ responseCode));
        } catch (Exception e) {
            listener.error(e);
        }finally {
            try {
                if (connection!=null)connection.disconnect();
                if (bufferedReader!=null)bufferedReader.close();
            } catch (IOException ignored) { }
        }
    }
    private String getResponse() throws IOException {
        bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder buffer = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null){
            buffer.append(line);
            if (line.contains("</title>"))break;
        }
        return String.valueOf(buffer);
    }

    /**
     * 参数转换函数
     * map -> http[post] 参数
     * @return string
     */
    private String formDataConnect() throws Exception {
        StringBuilder sb = new StringBuilder();
        for(String key:form.keySet()){
            if(sb.length() != 0){
                //从第二个参数开始，每个参数key、value前添加 & 符号
                sb.append("&");
            }
            String v = form.get(key);
            if ("deviceID".equals(key)&&v==null)throw new Exception("设备异常!无法查询设备id");
            sb.append(key).append("=").append(v);
        }
        return sb.toString();
    }
}
