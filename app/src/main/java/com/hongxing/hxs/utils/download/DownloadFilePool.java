package com.hongxing.hxs.utils.download;

import com.hongxing.hxs.utils.CommonUtils;
import com.hongxing.hxs.utils.http.HttpUtils;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class DownloadFilePool {
    // 网络资源路径
    private String urlLocation;
    // 存储路径
    private String filePath;
    // 多少个线程
    private int threads;
    private ExecutorService pool;
    private FileDownloadUtil.Listener listener;

    DownloadFilePool(String urlLocation, String filePath, int threads, FileDownloadUtil.Listener listener) {
        // 如果 保存路径为空则默认存在 D盘，文件名跟下载名相同
        if( filePath==null ) {
            String fileName = urlLocation.substring( urlLocation.lastIndexOf("/"));
            filePath = CommonUtils.getBackupPath() + fileName;
        }
        this.urlLocation = urlLocation;
        this.filePath = filePath;
        this.threads = threads;
        this.listener=listener;
    }

    void doDownload() {
        pool = Executors.newCachedThreadPool();
        new Thread(()->{
            try {
                listener.startDownload();
                // 获取文件长度
                String length = HttpUtils.getDoGetConnection(urlLocation+"?getLength=1").getHeaderField("length");
                if (length==null||"".equals(length))throw new RuntimeException("未获取到content_length下载失败");
                long fileLength =Long.parseLong(length);
//                System.out.println("文件大小："+length+" byte");
                FileDownloadUtil.sum = fileLength;
                // 获取每片大小
                long slice = fileLength/threads;
                for(int i = 0 ;i < threads; i++) {
                    long start = i*slice;
                    long end = (i+1)*slice -1;
                    if(i==threads-1) {
                        start = i*slice;
                        end =  fileLength ;
                    }
                    // 创建下载类
                    DownloadFileRang downloadFileRang;
                    if (threads>1)
                        downloadFileRang= new DownloadFileRang(""+i,start, end, urlLocation, filePath);
                    else downloadFileRang=new DownloadFileRang(""+i,urlLocation,filePath);
                    // 执行线程
                    pool.execute(downloadFileRang);
                }
                loop_detect();
            } catch (Exception e) {
//                e.printStackTrace();
                listener.error(e);
            }finally {
                // 关闭线程池
                pool.shutdown();
            }
        }).start();
    }

    private void loop_detect(){
        Runnable runnable = () -> {
            long old = 0,now,total=FileDownloadUtil.sum;
            Date startDate = new Date();
            while( true ) {
                now = FileDownloadUtil.process - old;
                old = FileDownloadUtil.process;

                if( FileDownloadUtil.process >= total ) {
                    long t = new Date().getTime() - startDate.getTime();
                    double speed = ((double)total / (t/1000.0))/1024.0;
                    System.out.println( "下载完成，用时：" + t/1000.0 +"秒,平均网速：" + speed +" kb/s" );
                    break;
                }
                double progress = (FileDownloadUtil.process / (double) total) * 100;
                listener.progress(progress);
                int speed = (int) ((now ) / 1024);
                listener.now_speed(speed+" kb/s");
//                System.out.println( "下载速度：" + speed +" kb/s,进度：" + progress +"%"  );
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    listener.error(e);
                }
            }
            listener.success();
        };
        pool.execute(runnable);
    }

}
