package com.hongxing.hxs.utils.http;

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
    private String url;
    private HttpUtils.Listener listener;
    private Map<String,String> form;
    private File file;
    private long fileSize;
    private int fileAction=0;
    private int requestMethod;
    private HttpURLConnection connection;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;

    HttpThread(String url, HttpUtils.Listener listener, String filePath) {
        this.url = url;
        this.listener = listener;
        this.requestMethod=POST;
        this.file = new File(filePath);
        this.fileAction=UPLOAD;
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
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(3000);
            connection.connect();
            int code = connection.getResponseCode();
            if (code ==200) listener.success(getResponse());
            else listener.error("responseCode "+code);
        } catch (Exception e) {e.printStackTrace();
            listener.error(e.getMessage());
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
        connection.setConnectTimeout(10000);
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
            if (file!=null&&file.exists()&&fileAction==UPLOAD){
                DataOutputStream dos = new DataOutputStream(outputStream);
                FileInputStream is = new FileInputStream(file);
                byte[] bytes = new byte[4 * 1024];int len;
//                long totalBytes = file.length();long curBytes = 0;
                while ((len=is.read(bytes))!=-1){
//                    curBytes += len;
                    dos.write(bytes,0,len);
//                    listener.onProgress(curBytes,1.0d *curBytes/totalBytes);
                }is.close();
                dos.flush();dos.close();
            }
            if (connection.getResponseCode()==200) {
                if (fileAction==DOWNLOAD){
                    if (!file.exists())file.createNewFile();
                    byte[] bytes = new byte[8 * 1024];int len;
                    BufferedInputStream bin = new BufferedInputStream(connection.getInputStream());
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                    HttpUtils.DownloadListener l = (HttpUtils.DownloadListener) listener;
                    l.startDownload();long curBytes = 0;
                    while ((len=bin.read(bytes))!=-1){
                        curBytes+=len;
                        bos.write(bytes,0,len);
                        updateProgress(l,curBytes);
                    }bos.flush();bos.close();bin.close();
                    l.success(file);
                }else
                listener.success("response:"+getResponse());
            }
            else listener.error("responseCode "+connection.getResponseCode());
        } catch (Exception e) {e.printStackTrace();
            listener.error(e.getMessage());
        }finally {
            try {
                if (connection!=null)connection.disconnect();
                if (bufferedReader!=null)bufferedReader.close();
                if (printWriter!=null)printWriter.close();
            } catch (IOException ignored) { }
        }
    }
    private int lastProgress = 0;
    private void updateProgress(HttpUtils.DownloadListener listener,long now){
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
        }
        return String.valueOf(buffer);
    }

    /**
     * 参数转换函数
     * map -> http[post] 参数
     * @return string
     */
    private String formDataConnect(){
        StringBuilder sb = new StringBuilder();
        for(String key:form.keySet()){
            if(sb.length() != 0){
                //从第二个参数开始，每个参数key、value前添加 & 符号
                sb.append("&");
            }
            String v = form.get(key);
            if ("deviceID".equals(key)&&v==null)throw new RuntimeException("设备异常!无法查询设备id");
            sb.append(key).append("=").append(v);
        }
        System.out.println(sb.toString());
        return sb.toString();
    }
}
