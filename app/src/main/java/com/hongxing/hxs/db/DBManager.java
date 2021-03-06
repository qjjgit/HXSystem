package com.hongxing.hxs.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.hongxing.hxs.MainActivity;
import com.hongxing.hxs.utils.zip.CompressListener;
import com.hongxing.hxs.utils.zip.ZIPUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DBManager {
    //数据库存储路径
    private static String DB_NAME = "hxs.s3db";
    //数据库存放的文件夹 SD卡 Android/data/com.hongxing.hxs/files/databases 下面
    private static String pathStr =MainActivity.APPStoragePath+File.separator+"databases";

    public static File db_file=new File(pathStr+File.separator+DB_NAME);
    public static long db_lastModified;

    public static SQLiteDatabase openDatabase(Context context){

        //查看数据库文件是否存在
        if(db_file.exists()){
//            Log.i("test", "存在数据库");
            //存在则直接返回打开的数据库
            return SQLiteDatabase.openOrCreateDatabase(db_file, null);
        }else{
            //不存在先创建文件夹
            File path=new File(pathStr);
//            Log.i("test", "pathStr="+pathStr);
            if (path.exists()){
                Log.i("test", "db文件已存在");
            }
            else if (path.mkdir()){
                Log.i("test", "创建成功");
            }else{
                Log.i("test", "创建失败");
            }
            createDBFile(context);
            initDBData(context);
            db_file.setLastModified(10);
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
            FileOutputStream fos=new FileOutputStream(db_file);
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
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(db_file, null);
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

    public static void exportDBFileToDir(String pathPrefix, CompressListener listener){
        String zipFilePath=pathPrefix+"备份.zip";
        ZIPUtils.compress(MainActivity.APPStoragePath,zipFilePath,listener);
    }
}
