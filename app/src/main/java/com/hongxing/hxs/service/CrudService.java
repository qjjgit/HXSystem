package com.hongxing.hxs.service;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.hongxing.hxs.db.DBManager;
import com.hongxing.hxs.entity.Goods;
import com.hongxing.hxs.entity.PurchaseOrder;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;

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

    public void savePurchaseOrder(int goodsId,PurchaseOrder purchaseOrder) {
        db.beginTransaction();
        try {
            String sql1="insert into pur_order('id','supplier','date','data_uri') values(?,?,?,?)";
            db.execSQL(sql1,new Object[]{purchaseOrder.getId(),purchaseOrder.getSupplier(),purchaseOrder.getDate(),purchaseOrder.getDataUri()});
            String sql2="insert into goods_pur_o('goods_id','pur_id') values(?,?)";
            db.execSQL(sql2,new Object[]{goodsId,purchaseOrder.getId()});
            db.setTransactionSuccessful();
        }finally {
            db.endTransaction();
        }
    }

    public ArrayList<PurchaseOrder> getPurOrderListByGoodsId(int goodsId) {
        String sql="select * from pur_order where id in(select pur_id from goods_pur_o where goods_id=?)";
        Cursor cursor= db.rawQuery(sql,new String[]{String.valueOf(goodsId)});
        ArrayList<PurchaseOrder> list = new ArrayList<>();
        while(cursor.moveToNext()){
            String id =cursor.getString(cursor.getColumnIndex("id"));
            String supplier =cursor.getString(cursor.getColumnIndex("supplier"));
            String dateStr=cursor.getString(cursor.getColumnIndex("date"));
            String dataUri=cursor.getString(cursor.getColumnIndex("data_uri"));
            list.add(new PurchaseOrder(id,supplier,dateStr,dataUri));
        }
        return list;
    }

    //删除数据的方法
    public void deleteGoodsById(int id) {
//        SQLiteDatabase db=databaseHelper.getWritableDatabase();
        String sql = "delete from goodsdata where id=?";
        db.execSQL(sql, new Object[]{id});
//        db.close();
    }

    //删除 进货单、对应的中间表、磁盘中的img文件
    public boolean deletePurOrder(PurchaseOrder purO){
        String sql1="delete from goods_pur_o where id=\""+purO.getId()+"\"";
        String sql2="delete from pur_order where id=\""+purO.getId()+"\"";
        try {
            db.beginTransaction();//开启事务
            db.execSQL(sql1);
            db.execSQL(sql2);
            File file = new File(purO.getDataUri());
            file.delete();
            db.setTransactionSuccessful();//声明事务成功
            return true;
        }catch (Exception e){
            return false;
        }finally {
            db.endTransaction();//结束事务
        }
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

    public Goods findByBarcode(String barcode) {//单条查询的方法
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

    public ArrayList<String> getDistinctGoodsList(){
        String sql = "select DISTINCT name from goodsdata";
        Cursor cursor= db.rawQuery(sql,null);
        ArrayList<String> list = new ArrayList<>();
        while(cursor.moveToNext()){
            String name =cursor.getString(cursor.getColumnIndex("name"));
            list.add(name);
        }
        return list;
    }

    //查询分页数据的方法
    public ArrayList<Goods> findByPage(int min,int page,String word,String orderBy,String sortAction) {
        ArrayList<Goods> list = new ArrayList<>();
        StringBuffer sql =new StringBuffer("select * from goodsdata ");
        if (word!=null&&!"".equals(word)){
            sql.append("where name like \"%").append(word).append("%\" or barcode = \"").append(word).append("\"");
        }
        if(orderBy!=null&&!"".equals(orderBy)){
            sql.append(" order by ").append(orderBy).append(" ").append(sortAction);
        }
        sql.append(" limit ?,?");
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
        return list;
    }

    public ArrayList<PurchaseOrder> findPurOrderByWord(String word){
        ArrayList<PurchaseOrder> list = new ArrayList<>();
        StringBuffer sql =new StringBuffer("select * from pur_order ");
        if (word!=null&&!"".equals(word)){
            sql.append("where supplier like \"%").append(word).append("%\"");
        }
        Cursor cursor= db.rawQuery(String.valueOf(sql),null);
        while(cursor.moveToNext()){
            String id =cursor.getString(cursor.getColumnIndex("id"));
            String supplier=cursor.getString(cursor.getColumnIndex("supplier"));
            String date=cursor.getString(cursor.getColumnIndex("date"));
            String uri=cursor.getString(cursor.getColumnIndex("data_uri"));
            list.add(new PurchaseOrder(id,supplier,date,uri));
        }
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
