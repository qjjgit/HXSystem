package com.hongxing.hxs.utils.zip;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
public class UnzipMainThread extends Thread {
    private String zipFileString;
    private String outPathString;
    private UnzipListener listener;
    UnzipMainThread(String zipFileString, String outPathString, UnzipListener listener) {
        this.zipFileString = zipFileString;
        this.outPathString = outPathString;
        this.listener = listener;
    }
    @Override
    public void run() {
        super.run();
        try {
            listener.zipStart();
            long sumLength = 0;
            // 获取解压之后文件的大小,用来计算解压的进度
            long zipLength = getZipTrueSize(zipFileString);
            FileInputStream inputStream = new FileInputStream(zipFileString);
            ZipInputStream inZip = new ZipInputStream(inputStream);
            ZipEntry zipEntry;
            String szName = "";
            while ((zipEntry = inZip.getNextEntry()) != null) {
                szName = zipEntry.getName();
                File f = new File(outPathString + File.separator +szName.substring(0, szName.lastIndexOf("/")));
                if (!f.exists())f.mkdirs();
                File file = new File(outPathString + File.separator + szName);
                if (file.exists()&&!szName.contains("hxs.s3db")) {
                    long length = file.length();
                    sumLength+=length;
                    int progress = (int) ((sumLength * 100) / zipLength);
                    updateProgress(progress);
                    continue;
                }
                else file.createNewFile();
                FileOutputStream out = new FileOutputStream(file);
                int len;
                byte[] buffer = new byte[1024];
                while ((len = inZip.read(buffer)) != -1) {
                    sumLength += len;
                    int progress = (int) ((sumLength * 100) / zipLength);
                    updateProgress(progress);
                    out.write(buffer, 0, len);
                    out.flush();
                }
                out.close();
            }
            listener.zipSuccess();
            inZip.close();
        } catch (Exception e) {
            listener.zipFail();
        }
    }
    private int lastProgress = 0;
    private void updateProgress(int progress) {
        /* 因为会频繁的刷新,这里我只是进度>1%的时候才去显示 */
        if (progress > lastProgress) {
            lastProgress = progress;
            listener.zipProgress(progress);
        }
    }
    /**
     * 获取压缩包解压后的内存大小
     *
     * @param filePath
     *      文件路径
     * @return 返回内存long类型的值
     */
    private long getZipTrueSize(String filePath) {
        long size = 0;
        ZipFile f;
        try {
            f = new ZipFile(filePath);
            Enumeration<? extends ZipEntry> en = f.entries();
            while (en.hasMoreElements()) {
                size += en.nextElement().getSize();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return size;
    }
}
