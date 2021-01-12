package com.hongxing.hxs.utils.download;

import com.hongxing.hxs.utils.http.HttpUtils;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;

class DownloadFileRang implements Runnable{
    private String id;
    // 文件开始位置
    private Long start ;
    // 文件结束位置
    private Long end;
    // url地址
    private String urlLocation;
    // 文件存储位置
    private String filePath;

    DownloadFileRang(String id, String urlLocation, String filePath) {
        this.id = id;
        this.urlLocation = urlLocation;
        this.filePath = filePath;
    }

    DownloadFileRang(String id, long start, long end, String urlLocation, String filePath) {
        super();
        this.id=id;
        this.start = start;
        this.end = end;
        this.urlLocation = urlLocation;
        this.filePath = filePath;
    }

    @Override
    public void run() {
        try {
            // 获取连接
            HttpURLConnection conn = HttpUtils.getDoGetConnection(urlLocation);
            // 设置获取资源范围
            if (start!=null&&end!=null) conn.setRequestProperty("Range",start +"-"+end );
            conn.connect();
            File file = new File(filePath);
            RandomAccessFile out = new RandomAccessFile(file, "rw");
            if (start!=null)out.seek(start);
            else out.seek(0);

            // 获取网络连接的 输入流
            InputStream is = conn.getInputStream();

            byte [] data = new byte[ (2048)];
            int len;
            while( (len = is.read(data))!=-1 ) {
                out.write(data, 0, len);
                synchronized (FileDownloadUtil.class) {
                    FileDownloadUtil.process += len;
                }
            }
//            System.out.println("线程"+id+"下载完成");
            // 关闭连接
            out.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
