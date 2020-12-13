package com.hongxing.hxs.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hongxing.hxs.MainActivity;
import com.hongxing.hxs.service.CrudService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Objects;
import java.util.UUID;

public class CommonUtils {
    public static String SERVERADDRESS=
            //"http://192.168.1.11:8080/hx_goods_system";
            "http://3579h68942.oicp.vip";


    public static String getAPPStoragePath(@NonNull Context context){
        return context.getExternalFilesDir(null).getPath();
    }

    /**
     * 获取cache路径
     * //@param context context
     * @return string
     */
    public static String getDiskCachePath(/*@NonNull Context context*/) {
        //自定义缓存目录
        String path = MainActivity.APPStoragePath + File.separator + "cache";
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

    public static String getDeviceID(@Nullable Context context){
        String deviceID=null;
        try {
            File file = new File(CommonUtils.getBackupPath() + "/.DeviceID");
            if (file.exists()){
                BufferedReader reader = new BufferedReader(new FileReader(file));
                deviceID = reader.readLine();reader.close();
            }
            else{
                CrudService service = new CrudService(context);
                deviceID = service.getDeviceID();service.close();
                if (deviceID==null) deviceID=createDeviceID(context);
                file.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(file,false));
                writer.write(deviceID);writer.flush();writer.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return deviceID;
//        CrudService service = new CrudService(context);
//        String deviceID = service.getDeviceID();service.close();
//        if (deviceID==null){
//            return createDeviceID(context);
//        }
//        return deviceID;
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    private static String createDeviceID(@Nullable Context context){
        String id=null;
        TelephonyManager mt = (TelephonyManager) Objects.requireNonNull(context).getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            if (mt != null) {
                String temp;
                try {
                    temp=mt.getMeid();
                } catch (Exception e) {
                    try {
                        temp=mt.getImei();
                    } catch (Exception ex) {
                        try {
                            temp=mt.getDeviceId();
                        }catch (Exception ey){
                            temp=null;
                        }
                    }
                }
                if (temp==null)id=UUID.randomUUID().toString();
                else id= UUID.nameUUIDFromBytes(temp.getBytes()).toString();
            }
        }else {
            if (mt != null) {
                id= UUID.nameUUIDFromBytes(mt.getDeviceId().getBytes()).toString();
            }
        }
        if (id != null) id=id.replaceAll("-","");
//        CrudService service = new CrudService(context);
//        service.saveDeviceID(id);service.close();
        return id;
    }
}
