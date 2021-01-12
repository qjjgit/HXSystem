package com.hongxing.hxs.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.UUID;

import static com.hongxing.hxs.MainActivity.APPStoragePath;

public class CommonUtils {
    private static String versionName;
    public static String SERVERADDRESS=
            //"http://192.168.1.3:8080/hx_goods_system";
            "http://3579h68942.oicp.vip";

    public static String getVersionName(Context context) {
        if (versionName!=null)return versionName;
        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }

    //android/data/packageName/files
    public static String getAPPStoragePath(@NonNull Context context){
        return context.getExternalFilesDir(null).getPath();
    }

    public static void checkRequiredFolder(){
        new Thread(()->{
            String appPath=APPStoragePath;
            File file;String[] list={"/cache","/databases","/PurchaseOrder"};
            for (String dir : list) {
                file=new File(appPath + dir);
                if (!file.exists())file.mkdirs();
            }
            file=new File(getBackupPath());
            if (!file.exists())file.mkdir();
        }).start();

    }

    /**
     * 获取cache路径    android/data/packageName/files/cache
     * //@param context context
     * @return string
     */
    public static String getDiskCachePath(/*@NonNull Context context*/) {
        //自定义缓存目录
        String path = APPStoragePath + File.separator + "cache";
        File file = new File(path);if (!file.exists())file.mkdirs();
        return path;

        //系统默认缓存目录
//        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
//                || !Environment.isExternalStorageRemovable()) {
//            return context.getExternalCacheDir().getPath();
//        } else {
//            return context.getCacheDir().getPath();
//        }
    }

    /**
     * @return 获取数据备份路径
     */
    public static String getBackupPath(){
        return Environment.getExternalStorageDirectory().getPath()
                + File.separator + "鸿兴系统";
    }

    public static String getDeviceID(){
        String deviceID=null;
        try {
            File file = new File(CommonUtils.getBackupPath() + "/.DeviceID");
            if (file.exists()){
                BufferedReader reader = new BufferedReader(new FileReader(file));
                deviceID = reader.readLine();reader.close();
            }
            else{
//                CrudService service = new CrudService(context);
//                deviceID = service.getDeviceID();service.close();
                deviceID=createDeviceID();
                file.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(file,false));
                writer.write(deviceID);writer.flush();writer.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return deviceID;
    }

    private static String createDeviceID(){
        String[] chars = new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n",
                "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7", "8",
                "9", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
                "U", "V", "W", "X", "Y", "Z" };
        StringBuilder buffer = new StringBuilder();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        for (int i = 0; i < 8; i++) {
            String str = uuid.substring(i * 4, i * 4 + 4);
            int x = Integer.parseInt(str, 16);
            buffer.append(chars[x % 0x3E]);
        }
        return buffer.toString();
    }
}
