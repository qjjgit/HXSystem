package com.hongxing.hxs.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.hongxing.hxs.MainActivity;
import com.hongxing.hxs.utils.ZIPUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DBManager {
    //数据库存储路径
    private static String DB_NAME = "hxs.s3db";
    //数据库存放的文件夹 SD卡 Android/data/com.hongxing.hxs/files/databases 下面
    private static String pathStr =MainActivity.APPStoragePath+File.separator+"databases";

    private static File jhPath=new File(pathStr+File.separator+DB_NAME);

    public static SQLiteDatabase openDatabase(Context context){

        //查看数据库文件是否存在
        if(jhPath.exists()){
//            Log.i("test", "存在数据库");
            //存在则直接返回打开的数据库
            return SQLiteDatabase.openOrCreateDatabase(jhPath, null);
        }else{
            //不存在先创建文件夹
            File path=new File(pathStr);
//            Log.i("test", "pathStr="+pathStr);
            if (path.exists()){
                Log.i("test", "已存在");
            }
            else if (path.mkdir()){
                Log.i("test", "创建成功");
            }else{
                Log.i("test", "创建失败");
            }
            createDBFile(context);
            initDBData(context);
            //如果没有这个数据库  我们已经把他写到SD卡上了，然后在执行一次这个方法 就可以返回数据库了
            return openDatabase(context);
        }
    }

    private static void createDBFile(Context context){
        try {
            //得到资源
            //AssetManager am= context.getAssets();
            //得到数据库的输入流
            InputStream is=context.getAssets().open(DB_NAME);
            //用输出流写到SDcard上面
            FileOutputStream fos=new FileOutputStream(jhPath);
            //创建byte数组  用于1KB写一次
            byte[] buffer=new byte[1024];
            int count = 0;
            while((count = is.read(buffer))>0){
                fos.write(buffer,0,count);
            }
            //最后关闭就可以了
            fos.flush();
            fos.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void initDBData(Context context){
//        CrudService service = new CrudService(context);
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(jhPath, null);
        try {
            InputStream in = context.getAssets().open("data.txt");
            int length = in.available();
            byte[] bytes=new byte[length];
            in.read(bytes);
            String sqls = new String(bytes, "GBK");
            String[] sqlList = sqls.split(";");
            for (String sql : sqlList) {
                db.execSQL(sql);
            }
            db.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean exportDBFileToDir(String dir){
        try {
        String zipFilePath=dir+File.separator
                +new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss备份", Locale.CHINA).format(new Date())
                +".zip";
            ZIPUtils.compress(MainActivity.APPStoragePath,zipFilePath);
            return true;
        }catch (Exception e){
            return false;
        }
//        try {
//            BufferedInputStream inputStream= new BufferedInputStream(new FileInputStream(jhPath));
//            String filePath=dir+File.separator
//                    +new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss备份").format(new Date())
//                    +".s3db";
//            BufferedOutputStream outputStream= new BufferedOutputStream(new FileOutputStream(filePath));
//            int j;
//            byte[] bytes2 =new byte[1024];
//            while((j=inputStream.read(bytes2))!=-1) {
//                outputStream.write(bytes2,0,j);
//            }
//            inputStream.close();
//            outputStream.close();
//            return true;
//        }catch (Exception e){
//            e.printStackTrace();
//            return false;
//        }
    }
}
