package com.hongxing.hxs.service;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.hongxing.hxs.MainActivity;
import com.hongxing.hxs.db.DBManager;
import com.hongxing.hxs.entity.Goods;
import com.hongxing.hxs.entity.PurchaseOrder;
import com.hongxing.hxs.utils.http.HttpUtils;

import java.io.File;
import java.util.ArrayList;

public class CrudService {
//    private DatabaseHelper databaseHelper;
    private SQLiteDatabase db;
    private boolean isModifiedOperation=false;
    public CrudService(Context context) {
//        databaseHelper=new DatabaseHelper(context);
        db=DBManager.openDatabase(context);
    }
    public void close(){
        if (db.isOpen())
            db.close();
        if (MainActivity.isAdmin&&isModifiedOperation){
            HttpUtils.uploadDBFile(new HttpUtils.Listener() {
                @Override
                public void startFileTransfer() { }
                @Override
                public void success(String response) {
                    System.out.println(response);
                }
                @Override
                public void error(Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
    public void execute(String sql){
        db.execSQL(sql);
    }

//    public String getDeviceID(){
//        String sql="select deviceID from system";
//        Cursor cursor = db.rawQuery(sql, null);
//        cursor.moveToNext();
//        return cursor.getString(cursor.getColumnIndex("deviceID"));
//    }
//    public void saveDeviceID(String id){
//        String sql="update system set deviceID=\""+id+"\"";
//        db.execSQL(sql);
//    }

    //增加数据的方法
    public void saveGoods(Goods goods){
        String sql="insert into goodsdata('name','barcode','unit','price','orig') values(?,?,?,?,?)";
//        byte[] bytes = goods.getName().getBytes();
//        String name = new String(bytes, "GBK");
        db.execSQL(sql, new Object[]{goods.getName(),goods.getBarcode(),goods.getUnit(),goods.getPrice(),goods.getOrig()});//执行sql语句？由数组提供
        isModifiedOperation=true;
    }

    //增加进货单
    public void savePurchaseOrder(Goods goods,PurchaseOrder purchaseOrder) {
        db.beginTransaction();
        try {
            String sql0="insert into pur_order('id','supplier','date','data_uri') values(?,?,?,?)";
            db.execSQL(sql0,new Object[]{purchaseOrder.getId(),purchaseOrder.getSupplier(),purchaseOrder.getDate(),purchaseOrder.getDataUri()});
            if (goods!=null){
                String sql1="select id from goodsdata where name=\""+goods.getName()+"\"";
                Cursor cursor= db.rawQuery(sql1,null);
                while(cursor.moveToNext()){
                    int id =cursor.getInt(cursor.getColumnIndex("id"));
                    String sql="insert into goods_pur_o('goods_id','pur_id') values(?,\""+purchaseOrder.getId()+"\")";
                    db.execSQL(sql,new Object[]{id});
                }
            }
            db.setTransactionSuccessful();
            isModifiedOperation=true;
        }finally {
            db.endTransaction();
        }
    }

    //通过商品id获取该商品的进货单list
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

    //删除 进货单、对应的中间表、磁盘中的img文件和缓存
    public boolean deletePurOrder(PurchaseOrder purO){
        String sql1="delete from goods_pur_o where pur_id=\""+purO.getId()+"\"";
        String sql2="delete from pur_order where id=\""+purO.getId()+"\"";
        try {
            db.beginTransaction();//开启事务
            db.execSQL(sql1);
            db.execSQL(sql2);
            File img = new File(purO.getDataUri());
            if (img.exists())img.delete();
            File cache = new File(purO.getCachePath());
            if (cache.exists())cache.delete();
            db.setTransactionSuccessful();//声明事务成功
            isModifiedOperation=true;
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
        isModifiedOperation=true;
    }

    //更新进货单
    public void updatePurOrder(PurchaseOrder pur){
        String sql="update pur_order set supplier=\""+pur.getSupplier()+"\","
                    +"date=\""+pur.getDate()+"\" where id=\""+pur.getId()+"\"";
        db.execSQL(sql);isModifiedOperation=true;
    }

    //是否存在 name和unit的商品
    public boolean existGoodsByNameAndUnit(String name,String unit){
        boolean exist=false;
//        byte[] bytes = name.getBytes();
//        String name_u = new String(bytes, "GBK");
        String sql = "select * from goodsdata where name = \""+name+"\" and unit= \""+unit+"\"";
        Cursor cursor= db.rawQuery(sql,null);
        if (cursor.getCount()>0)exist=true;
        return exist;
    }

    //通过barcode查询商品
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

    /*获取去重(name重复)后的商品集合GoodsList*/
    public ArrayList<String> getDistinctGoodsNameList(String word){
        StringBuffer sql = new StringBuffer("select DISTINCT name from goodsdata ");
        if (word!=null&&!"".equals(word)){
            sql.append("where name like \"%").append(word).append("%\" or barcode = \"").append(word).append("\"");
        }
        Cursor cursor= db.rawQuery(String.valueOf(sql),null);
        ArrayList<String> list = new ArrayList<>();
        while(cursor.moveToNext()){
            String name =cursor.getString(cursor.getColumnIndex("name"));
            list.add(name);
        }
        return list;
    }

    //通过进货单id获取当前进货单所绑定的goods的name集合
    public ArrayList<String> getGoodsNameListByPurOrderId(String id){
        String sql = "select DISTINCT name from goodsdata where id in" +
                "(select goods_id from goods_pur_o where pur_id=\""+id+"\")";
        Cursor cursor= db.rawQuery(sql,null);
        ArrayList<String> list = new ArrayList<>();
        while(cursor.moveToNext()){
            String name =cursor.getString(cursor.getColumnIndex("name"));
            list.add(name);
        }
        return list;
    }

    //通过goodsName绑定goods、purOrder
    public boolean bindingGoods2PurOrder(ArrayList<String> nameList,String purOrderId){
        db.beginTransaction();
        try {
            String sql0="delete from goods_pur_o where pur_id=\""+purOrderId+"\"";
            db.execSQL(sql0);//先清除旧数据
            //相同的goods name都绑定同一个进货单
            for (String name : nameList) {
                String sql1="select id from goodsdata where name=\""+name+"\"";
                Cursor cursor= db.rawQuery(sql1,null);
                while(cursor.moveToNext()){
                    int id =cursor.getInt(cursor.getColumnIndex("id"));
                    String sql="insert into goods_pur_o('goods_id','pur_id') values(?,\""+purOrderId+"\")";
                    db.execSQL(sql,new Object[]{id});
                }
            }
            isModifiedOperation=true;
            db.setTransactionSuccessful();
            return true;
        }catch (Exception e){
            return false;
        }finally {
            db.endTransaction();
        }
    }

    //查询分页数据的方法
    public ArrayList<Goods> findByPage(int min,int page,String word) {
        ArrayList<Goods> list = new ArrayList<>();
        StringBuffer sql =new StringBuffer("select * from goodsdata ");
        if (word!=null&&!"".equals(word)){
            sql.append("where name like \"%").append(word).append("%\" or barcode = \"").append(word).append("\"");
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

    //查询进货单
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

    //更新 最后备份时间
    public void setLastBackup(String date){
        String sql="update system set last_backup=\""+date+"\"";
        db.execSQL(sql);
    }

    // 最后备份时间
    public String getLastBackup(){
        String sql="select last_backup from system";
        Cursor cursor = db.rawQuery(sql, null);
        cursor.moveToNext();
        return cursor.getString(cursor.getColumnIndex("last_backup"));
    }

    //统计商品数据条数
    public int getGoodsCount() {
        String sql="select id from goodsdata";
        Cursor cursor = db.rawQuery(sql, null);
        int count = cursor.getCount();
        return count;
    }

    //统计进货单数据条数
    public int getPurOrderCount(){
        String sql="select id from pur_order";
        Cursor cursor = db.rawQuery(sql, null);
        int count = cursor.getCount();
        return count;
    }

    //解除绑定进货单
    public boolean unlinkPurOrder(Goods goods, PurchaseOrder pur) {
        db.beginTransaction();
        try {
            String sql="delete from goods_pur_o where pur_id=\""+pur.getId()+"\" and goods_id=?";
            db.execSQL(sql,new Object[]{goods.getId()});
            db.setTransactionSuccessful();
            isModifiedOperation=true;
            return true;
        }catch (Exception e){
            return false;
        }finally {
            db.endTransaction();
        }
    }

    //绑定进货单 到 商品
    public boolean bindingPurOrder2Goods(ArrayList<PurchaseOrder> purChoices, Integer goods_id) {
        db.beginTransaction();
        try {
            String sql0="delete from goods_pur_o where goods_id="+goods_id;
            db.execSQL(sql0);//先清除旧数据
            for (PurchaseOrder pur : purChoices) {
                String sql="insert into goods_pur_o('goods_id','pur_id') values(?,\""+pur.getId()+"\")";
                db.execSQL(sql,new Object[]{goods_id});
            }
            isModifiedOperation=true;
            db.setTransactionSuccessful();
            return true;
        }catch (Exception e){
            return false;
        }finally {
            db.endTransaction();
        }
    }
}
