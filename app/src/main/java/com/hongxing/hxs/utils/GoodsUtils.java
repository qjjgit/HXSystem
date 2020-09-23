package com.hongxing.hxs.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

import com.hongxing.hxs.R;
import com.hongxing.hxs.entity.Goods;
import com.hongxing.hxs.service.CrudService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;


public class GoodsUtils{
    public final static int DO_ADD=0;
    public final static int DO_UPDATE=1;
    public static boolean checkGoodsInfoForAction(Context context, Goods goods, HashMap<String,String> map, int action) {
        ArrayList<String> list = new ArrayList<>();
        if ("".equals(map.get("name"))){
            list.add(" 商品名称不能为空！");
        }else goods.setName(map.get("name"));
        if ("".equals(map.get("barcode"))
            ||"无".equals(map.get("barcode"))){
            goods.setBarcode("无");
        }else{
            if (map.get("barcode").length()!=13)
                list.add(" 请输入正确的13位商品条码！");
            goods.setBarcode(map.get("barcode"));
        }
        if ("请选择".equals(map.get("unit"))){
            list.add(" 请选择商品单位！");
        }else goods.setUnit(map.get("unit"));
        if ("".equals(map.get("price"))){
            list.add(" 商品售价不能为空！");
        }else goods.setPrice(Float.valueOf(Objects.requireNonNull(map.get("price"))));
        if ("".equals(map.get("orig"))){
            map.put("orig","0.00");
        }else{
            if (!"".equals(map.get("price"))){
                float price = Float.valueOf(Objects.requireNonNull(map.get("price")));
                float orig = Float.valueOf(Objects.requireNonNull(map.get("orig")));
                if (orig>price) list.add(" 售价不能低于进货价！");
            }
        }goods.setOrig(Float.valueOf(Objects.requireNonNull(map.get("orig"))));

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle("提示");
        dialog.setIcon(R.drawable.error);
        CrudService service = new CrudService(context);
        if(!list.isEmpty()){
            dialog.setMessage(list.get(0));
            dialog.show();
            service.close();
            return false;
        }else{
            String name = map.get("name");
            String unit = map.get("unit");
            boolean exist = service.existGoodsByNameAndUnit(name,unit);
            if (exist){
                dialog.setMessage("已存在名称为 “"+name+"” 且单位为 “"+unit+"” 的商品,请修改名称或单位！");
                dialog.show();
                service.close();
                return false;
            }
            switch (action){
                case DO_ADD:{
                    service.save(goods);
                    Toast.makeText(context, "添加成功！", Toast.LENGTH_LONG).show();
                    service.close();
                    return true;
                }
                case DO_UPDATE:{
                    service.update(goods);
                    Toast.makeText(context, "商品信息更新成功！", Toast.LENGTH_LONG).show();
                    service.close();
                    return true;
                }
                default:return true;
            }
        }
    }
}
