package com.hongxing.hxs.utils.http;

import android.graphics.BitmapFactory;

import com.hongxing.hxs.MainActivity;
import com.hongxing.hxs.utils.zip.ZIPUtils;

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
    private final static int DOWNLOAD = 3;
    final static int UPLOADIMG = 4;
    private String url;
    private HttpUtils.Listener listener;
    private Map<String,String> form;
    private File file;
    private long fileSize;
    private int fileAction=0;
    private int requestMethod;
    private HttpURLConnection connection;
    private BufferedReader bufferedReader;

    HttpThread(String url, HttpUtils.Listener listener, String filePath) {
        this.url = url;
        this.listener = listener;
        this.requestMethod=POST;
        this.file = new File(filePath);
        this.fileAction=UPLOAD;
    }
    HttpThread(String url, HttpUtils.Listener listener, String filePath,int fileAction) {
        this.url = url;
        this.listener = listener;
        this.requestMethod=POST;
        this.file = new File(filePath);
        this.fileAction=fileAction;
    }
    HttpThread(String url, HttpUtils.DownloadListener listener,Map<String, String> form, String filePath,long dataLength) {
        this.url = url;
        this.listener = listener;
        this.form=form;
        this.requestMethod=POST;
        this.file = new File(filePath);
        this.fileSize=dataLength;
        this.fileAction=DOWNLOAD;
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
//        connection.setRequestProperty("Content-Type", "multipart/form-data" + ";boundary=" + BOUNDARY);
//            connection.setRequestProperty("Content-type","application/x-java-serialized-object");
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
                listener.startFileTransfer();
                ZIPUtils.compress(MainActivity.APPStoragePath,file.getPath());
                this.fileSize=file.length();
                DataOutputStream dos = new DataOutputStream(outputStream);
                FileInputStream is = new FileInputStream(file);
                byte[] bytes = new byte[2*1024 * 1024];int len;
                long curBytes = 0;
                while ((len=is.read(bytes))!=-1){
                    curBytes += len;
                    dos.write(bytes,0,len);
                    updateProgress(listener,curBytes);
                }is.close();
                dos.flush();dos.close();
            }
            if (file!=null&&fileAction==UPLOADIMG){
                DataOutputStream dos = new DataOutputStream(outputStream);
                FileInputStream is = new FileInputStream(file);
                byte[] bytes = new byte[2*1024 * 1024];int len;
                while ((len=is.read(bytes))!=-1){
                    dos.write(bytes,0,len);
                }is.close();dos.flush();dos.close();
            }
            if (connection.getResponseCode()==200) {
                if (fileAction==DOWNLOAD){
                    listener.startFileTransfer();
                    File parent = file.getParentFile();
                    if (!parent.exists())parent.mkdirs();
                    if (!file.exists()){if (file.createNewFile()) System.out.println("create temp.zip");else
                        System.out.println("not create temp.zip");}
                    byte[] bytes = new byte[2* 1024 * 1024];int len;
                    BufferedInputStream bin = new BufferedInputStream(connection.getInputStream());
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                    long curBytes = 0;
                    while ((len=bin.read(bytes))!=-1){
                        curBytes+=len;
                        bos.write(bytes,0,len);
                        updateProgress(listener,curBytes);
                    }bos.flush();bos.close();bin.close();
                    ((HttpUtils.DownloadListener) listener).success(file);
                }else if (fileAction==UPLOAD){
                    file.delete();
                    listener.success("response:"+getResponse());
                }else
                listener.success("response:"+getResponse());
            }
            else listener.error(new Exception("responseCode "+connection.getResponseCode()));
        } catch (Exception e) {
            listener.error(e);
        }finally {
            try {
                if (connection!=null)connection.disconnect();
                if (bufferedReader!=null)bufferedReader.close();
            } catch (IOException ignored) { }
        }
    }
    private int lastProgress = 0;
    private void updateProgress(HttpUtils.Listener listener,long now){
        int progress = (int) ((now * 100) / fileSize);
        if (progress > lastProgress) {
            lastProgress = progress;
            listener.progress(progress);
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
