package com.hongxing.hxs.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZIPUtils {
    /**s
     * 压缩文件
     * @param srcFilePath 压缩源路径
     * @param destFilePath 压缩目的路径
     */
    public static void compress(String srcFilePath, String destFilePath) throws IOException {
        File src = new File(srcFilePath);
        if (!src.exists()) {
            throw new RuntimeException(srcFilePath + "不存在");
        }
        File zipFile = new File(destFilePath);
        FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos);
        String baseDir = "";
        compressByType(src, zos, baseDir);
        zos.close();
    }
    /**
     * 按照原路径的类型就行压缩。文件路径直接把文件压缩，
     * @param src
     * @param zos
     * @param baseDir
     */
    private static void compressByType(File src, ZipOutputStream zos,String baseDir) throws IOException {
        if (!src.exists())return;
        System.out.println("压缩路径" + baseDir + src.getName());
        //判断文件是否是文件，如果是文件调用compressFile方法,如果是路径，则调用compressDir方法；
        if (src.isFile()) {
            //src是文件，调用此方法
            compressFile(src, zos, baseDir);
        } else if (src.isDirectory()) {
            //src是文件夹，调用此方法
            compressDir(src, zos, baseDir);
        }
    }

    /**
     * 压缩文件
     */
    private static void compressFile(File file, ZipOutputStream zos,String baseDir) throws IOException {
        if (!file.exists())return;
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        ZipEntry entry = new ZipEntry(baseDir + file.getName());
        zos.putNextEntry(entry);
        int count;
        byte[] buf = new byte[1024];
        while ((count = bis.read(buf)) != -1) {
            zos.write(buf, 0, count);
        }
        bis.close();
    }

    /**
     * 压缩文件夹
     */
    private static void compressDir(File dir, ZipOutputStream zos,String baseDir) throws IOException {
        if (!dir.exists())return;
        File[] files = dir.listFiles();
        if(files.length == 0){
            zos.putNextEntry(new ZipEntry(baseDir + dir.getName()+File.separator));
        }
        for (File file : files) {
            compressByType(file, zos, baseDir + dir.getName() + File.separator);
        }
    }

    /**
     * 解压缩zip文件，耗时操作，建议放入异步线程
     *
     * */
    public static void unzip(String targetPath, String zipFilePath) {
        try {
            int BUFFER = 2048;
            ZipFile zipFile = new ZipFile(zipFilePath);
            Enumeration emu = zipFile.entries();
            int i = 0;
            while (emu.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) emu.nextElement();
                if (entry.isDirectory()) {
                    new File(targetPath + entry.getName()).mkdirs();
                    continue;
                }
                BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
                File file = new File(targetPath + entry.getName());
                File parent = file.getParentFile();
                if (parent != null && (!parent.exists())) {
                    parent.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER);
                int count;
                byte[] data = new byte[BUFFER];
                while ((count = bis.read(data, 0, BUFFER)) != -1) {
                    bos.write(data, 0, count);
                }
                bos.flush();
                bos.close();
                bis.close();
            }
            zipFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}