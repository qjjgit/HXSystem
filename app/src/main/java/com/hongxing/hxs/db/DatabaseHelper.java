package com.hongxing.hxs.db;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG="DatabaseHelper";
    private static String DATABASE_NAME="hxs.s3db";
    private static int DATABASE_VERSION=1;

    public DatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public DatabaseHelper(Context context, String name, int version)
    {
        super(context, name, null, version);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
//  创建数据库后，对数据库的操作
        Log.e(TAG,"开始创建数据库表");
        try {
            db.execSQL("create table if not exists goodsdata(id integer primary key autoincrement ,name char(20),barcode char(15),unit char(4),price float,orig float)");
            Log.e(TAG,"创建离线所需数据库表成功");
        }catch (SQLException e){
            e.printStackTrace();
            Log.e(TAG,"创建离线所需数据库表失败");
        }
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//  更改数据库版本的操作
        db.execSQL("alter table goods add tel varchar(20)");
    }
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
//  每次成功打开数据库后首先被执行
    }
}
