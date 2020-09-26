package com.hongxing.hxs.service;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


import com.hongxing.hxs.db.DBManager;
import com.hongxing.hxs.entity.Goods;
import com.hongxing.hxs.entity.PurchaseOrder;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class CrudService {
//    private DatabaseHelper databaseHelper;
    private SQLiteDatabase db;
    public CrudService(Context context) {
//        databaseHelper=new DatabaseHelper(context);
        db=DBManager.openDatabase(context);
    }
    public void close(){
        if (db.isOpen())
            db.close();
    }
    public void execute(String sql){
        db.execSQL(sql);
    }

    //增加数据的方法
    public void saveGoods(Goods goods){
//        SQLiteDatabase db= DBManager.openDatabase()//获取数据实体//写方法//会判断是否数据库已经满了
        String sql="insert into goodsdata('name','barcode','unit','price','orig') values(?,?,?,?,?)";
//        byte[] bytes = goods.getName().getBytes();
//        String name = new String(bytes, "GBK");
        db.execSQL(sql, new Object[]{goods.getName(),goods.getBarcode(),goods.getUnit(),goods.getPrice(),goods.getOrig()});//执行sql语句？由数组提供
    }

    public void savePurchaseOrder(int goodsId,PurchaseOrder purchaseOrder){
        String sql1="insert into pur_order('id','supplier','date','data') values(?,?,?,?)";
        db.execSQL(sql1,new Object[]{purchaseOrder.getId(),purchaseOrder.getSupplier(),purchaseOrder.getDate(),purchaseOrder.getData()});

        String sql2="insert into goods_pur_o('goods_id','pur_id') values(?,?)";
        db.execSQL(sql2,new Object[]{goodsId,purchaseOrder.getId()});
    }

    public ArrayList<PurchaseOrder> getPurOrderListByGoodsId(int goodsId) throws ParseException {
        String sql="select * from pur_order where id in(select pur_id from goods_pur_o where goods_id=?)";
        Cursor cursor= db.rawQuery(sql,new String[]{String.valueOf(goodsId)});
        ArrayList<PurchaseOrder> list = new ArrayList<>();
        while(cursor.moveToNext()){
            String id =cursor.getString(cursor.getColumnIndex("id"));
            String supplier =cursor.getString(cursor.getColumnIndex("supplier"));
            String dateStr=cursor.getString(cursor.getColumnIndex("date"));
            byte[] data=cursor.getBlob(cursor.getColumnIndex("data"));
            list.add(new PurchaseOrder(id,supplier,dateStr,data));
        }
        return list;
    }

    //删除数据的方法
    public void deleteById(int id) {
//        SQLiteDatabase db=databaseHelper.getWritableDatabase();
        String sql = "delete from goodsdata where id=?";
        db.execSQL(sql, new Object[]{id});
//        db.close();
    }

    //修改数据的方法
    public void updateGoods(Goods goods) {
        String sql = "update goodsdata set name=?,barcode=? ,unit=?,price=?,orig=? where id=?";
        db.execSQL(sql,new Object[]{
                goods.getName(),goods.getBarcode(),goods.getUnit(),goods.getPrice(),goods.getOrig(),goods.getId()});
        db.close();
    }

    public Goods findById(int id) {//单条查询的方法
        Goods goods=null;
//        SQLiteDatabase db=databaseHelper.getReadableDatabase();
        String sql = "select * from goodsdata where id=?";
        Cursor cursor= db.rawQuery(sql,new String[]{String.valueOf(id)});
        if(cursor.moveToFirst()){
            int id2 =cursor.getInt(cursor.getColumnIndex("id"));
            String name=cursor.getString(cursor.getColumnIndex("name"));
            String barcode=cursor.getString(cursor.getColumnIndex("barcode"));
            String unit=cursor.getString(cursor.getColumnIndex("unit"));
            float price=cursor.getFloat(cursor.getColumnIndex("price"));
            float orig=cursor.getFloat(cursor.getColumnIndex("orig"));
            goods=new Goods(id2,name,barcode,unit,price,orig);
        }
//        db.close();
        return goods;
    }

    public boolean existGoodsByNameAndUnit(String name,String unit){
        boolean exist=false;
//        byte[] bytes = name.getBytes();
//        String name_u = new String(bytes, "GBK");
        String sql = "select * from goodsdata where name = \""+name+"\" and unit= \""+unit+"\"";
        Cursor cursor= db.rawQuery(sql,null);
        if (cursor.getCount()>0)exist=true;
        return exist;
    }

    public Goods findByBarcode(String barcode) throws UnsupportedEncodingException {//单条查询的方法
        Goods goods=null;
//        SQLiteDatabase db=databaseHelper.getReadableDatabase();
        String sql = "select * from goodsdata where barcode=?";
        Cursor cursor= db.rawQuery(sql,new String[]{barcode});
        if(cursor.getCount()<1)return null;
        if(cursor.moveToFirst()){
            int id =cursor.getInt(cursor.getColumnIndex("id"));
            String name=cursor.getString(cursor.getColumnIndex("name"));
//            String name=new String(val_name,"GBK");
            String unit=cursor.getString(cursor.getColumnIndex("unit"));
//            String unit=new String(val_unit,"GBK");
            float price=cursor.getFloat(cursor.getColumnIndex("price"));
            float orig=cursor.getFloat(cursor.getColumnIndex("orig"));
            goods=new Goods(id,name,barcode,unit,price,orig);
        }
//        db.close();
        return goods;
    }

    //查询分页数据的方法
    public ArrayList<Goods> findByPage(int min,int page,String word) {
        ArrayList<Goods> list = new ArrayList<>();
        StringBuffer sql =new StringBuffer("select * from goodsdata ");
        if (word!=null&&!"".equals(word)){
            sql.append("where name like \"%").append(word).append("%\" or barcode = \"").append(word).append("\"");
        }sql.append(" limit ?,?");
        Cursor cursor= db.rawQuery(String.valueOf(sql),new String[]{String.valueOf(min), String.valueOf(page)});
        while(cursor.moveToNext()){
            int id =cursor.getInt(cursor.getColumnIndex("id"));
            String name=cursor.getString(cursor.getColumnIndex("name"));
            String barcode=cursor.getString(cursor.getColumnIndex("barcode"));
            String unit=cursor.getString(cursor.getColumnIndex("unit"));
            float price=cursor.getFloat(cursor.getColumnIndex("price"));
            float orig=cursor.getFloat(cursor.getColumnIndex("orig"));
            list.add(new Goods(id,name,barcode,unit,price,orig));
        }
//        db.close();
        return list;
    }

    //统计数据条数
    public int getCount() {
//        SQLiteDatabase db=databaseHelper.getReadableDatabase();
        String sql="select id from goodsdata";
        Cursor cursor = db.rawQuery(sql, null);
        int count = cursor.getCount();
//        db.close();
        return count;
    }
}
