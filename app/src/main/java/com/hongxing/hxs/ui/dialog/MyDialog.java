package com.hongxing.hxs.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.hongxing.hxs.MainActivity;
import com.hongxing.hxs.R;
import com.hongxing.hxs.entity.Goods;
import com.hongxing.hxs.utils.GoodsUtils;

import java.util.HashMap;

public class MyDialog{
    //添加商品弹窗
    public static void showAddGoodsPage(final Context context){
        View view= LayoutInflater.from(context).inflate(R.layout.dialog_add_goods_page, null);
        final TextView cancel =view.findViewById(R.id.addGoods_cancel);
        final TextView sure =view.findViewById(R.id.addGoods_sure);
        final EditText eText_name =view.findViewById(R.id.addGoods_name);
        final Spinner spinner_unit =view.findViewById(R.id.addGoods_unitList);
        final TextView tView_barcode=view.findViewById(R.id.addGoods_barcode);
        final EditText eText_price =view.findViewById(R.id.addGoods_price);
        final EditText eText_orig =view.findViewById(R.id.addGoods_orig);
        AlertDialog.Builder builder= new AlertDialog.Builder(context);
        final Dialog dialog= builder.create();
        dialog.show();
        dialog.getWindow().setContentView(view);
        dialog.setCancelable(false);
        if (MainActivity.goods==null)MainActivity.goods=new Goods();
        if (MainActivity.goods.getBarcode()!=null){
            tView_barcode.setText(MainActivity.goods.getBarcode());
            tView_barcode.setFocusable(false);
        }
        //使editext可以唤起软键盘
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        spinner_unit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String item = spinner_unit.getSelectedItem().toString();
                MainActivity.goods.setUnit(item);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                MainActivity.goods.setUnit("个");
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.goods=null;
                dialog.dismiss();
            }
        });
        sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashMap<String, String> map = new HashMap<>();
                map.put("name",eText_name.getText().toString());
                map.put("barcode",tView_barcode.getText().toString());
                map.put("unit",MainActivity.goods.getUnit());
                map.put("price",eText_price.getText().toString());
                map.put("orig",eText_orig.getText().toString());
                boolean ok = GoodsUtils.checkGoodsInfoForAction(context,MainActivity.goods,map,GoodsUtils.DO_ADD);
                if (ok) dialog.dismiss();
            }
        });
    }
}