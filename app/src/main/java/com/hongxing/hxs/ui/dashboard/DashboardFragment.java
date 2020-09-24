package com.hongxing.hxs.ui.dashboard;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.hongxing.hxs.MainActivity;
import com.hongxing.hxs.R;
import com.hongxing.hxs.entity.Goods;
import com.hongxing.hxs.service.CrudService;
import com.hongxing.hxs.utils.GoodsUtils;

import java.lang.reflect.Array;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class DashboardFragment extends Fragment {

    private LinearLayout tableHeader;
    private LinearLayout tableBody;
    private RelativeLayout relativeLayout;
    private String[] tableHeaderTexts={"序号","商品名称","单位","售价","进货价"};
    private ArrayList<ArrayList<MyTableTextView>> tableBodyList=new ArrayList<>();
    private ArrayList<Goods> tableBodyDataList;
    private DashboardViewModel dashboardViewModel;
    final Handler uiHandler=new Handler();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                ViewModelProviders.of(this).get(DashboardViewModel.class);
        final View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
//        final TextView textView = root.findViewById(R.id.text_dashboard);
        dashboardViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
            tableHeader=root.findViewById(R.id.MyTableHeader);
            tableBody=root.findViewById(R.id.MyTable);
                final EditText searchTextV = root.findViewById(R.id.btn_search);
                searchTextV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                String str = (searchTextV.getText().toString()).replace(" ","");
                searchTextV.setText(str);
                doSearch(str);
                }
            });
            AsynchronousLoading();
            }
        });
        return root;
    }
    private void AsynchronousLoading(){
        final ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setProgress(0);
        progressDialog.setMessage("加载中...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMax(10);
        progressDialog.show();
        final Thread t = new Thread() {
            public void run(){
                boolean post = uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        initTableHeader();
                        LoadData(null);
                    }
                });
                if (post){
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    progressDialog.setProgress(10);
                    progressDialog.cancel();
                }
            }
        };
        t.start();
    }

    private void initTableHeader(){
        relativeLayout=(RelativeLayout)LayoutInflater.from(this.getContext()).inflate(R.layout.table,null);
        MyTableTextView title=relativeLayout.findViewById(R.id.list_1_1);
        title.setText(tableHeaderTexts[0]);
        title.setTextColor(Color.BLUE);
        setOnClick(title);
        title=relativeLayout.findViewById(R.id.list_1_2);
        title.setText(tableHeaderTexts[1]);
        title.setTextColor(Color.BLUE);
        setOnClick(title);
        title=relativeLayout.findViewById(R.id.list_1_3);
        title.setText(tableHeaderTexts[2]);
        title.setTextColor(Color.BLUE);
        setOnClick(title);
        title=relativeLayout.findViewById(R.id.list_1_4);
        title.setText(tableHeaderTexts[3]);
        title.setTextColor(Color.BLUE);
        setOnClick(title);
        title=relativeLayout.findViewById(R.id.list_1_5);
        title.setText(tableHeaderTexts[4]);
        title.setTextColor(Color.BLUE);
        setOnClick(title);
        /*表头*/
        tableHeader.addView(relativeLayout);
    }

    private void LoadData(String searchWord){
        final CrudService service = new CrudService(getContext());
        tableBodyDataList = service.findByPage(0, service.getCount(),searchWord);
        service.close();
        if(tableBodyDataList.size()<1){
            TextView child = new TextView(getContext());
            child.setTextSize(20);
            child.setWidth(360);
            child.setPadding(10,20,0,0);
            child.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            child.setText("没有查询到相关商品！");
            tableBody.addView(child);
            return;
        }
        for(int i=0;i<tableBodyDataList.size();i++){
            relativeLayout=(RelativeLayout) LayoutInflater.from(this.getContext()).inflate(R.layout.table,null);
            int color = Color.parseColor("#ffffff");
            if (i%2!=0) color= Color.parseColor("#eeeeee");
            final MyTableTextView col1 = relativeLayout.findViewById(R.id.list_1_1);
            col1.setBackgroundColor(color);
            col1.setText(String.valueOf(i+1));
            final MyTableTextView col2 = relativeLayout.findViewById(R.id.list_1_2);
            col2.setBackgroundColor(color);
            col2.setText(tableBodyDataList.get(i).getName());
            final MyTableTextView col3 = relativeLayout.findViewById(R.id.list_1_3);
            col3.setBackgroundColor(color);
            col3.setText(tableBodyDataList.get(i).getUnit());
            final MyTableTextView col4 = relativeLayout.findViewById(R.id.list_1_4);
            col4.setBackgroundColor(color);
            col4.setText(String.valueOf(tableBodyDataList.get(i).getPrice()));
            final MyTableTextView col5 = relativeLayout.findViewById(R.id.list_1_5);
            col5.setBackgroundColor(color);
            col5.setText(String.valueOf(tableBodyDataList.get(i).getOrig()));
            tableBodyList.add(new ArrayList<MyTableTextView>(){{
                add(col1);add(col2);add(col3);add(col4);add(col5);
            }});
            final int rowNumber = i;
            relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                 showGoodsInfoPage(rowNumber);
                }
            });
            tableBody.addView(relativeLayout);
        }
    }

    private void setOnClick(MyTableTextView view){
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {// TODO: 2020/9/24 点击表头实现排序
                switch (view.getId()){
                    case R.id.list_1_1:
                        Toast.makeText(getContext(),"点击了序号",Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.list_1_2:
                        Toast.makeText(getContext(),"点击了名称",Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.list_1_3:
                        Toast.makeText(getContext(),"点击了单位",Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.list_1_4:
                        Toast.makeText(getContext(),"点击了售价",Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.list_1_5:
                        Toast.makeText(getContext(),"点击了进货价",Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    private void doSearch(String searchWord){
        if(searchWord.length()==1){
            Toast.makeText(getContext(),"至少输入2位字符进行搜索！",Toast.LENGTH_SHORT).show();
            return;
        }
        if ("".equals(searchWord)){
            CrudService service = new CrudService(getContext());
            int count = service.getCount();
            service.close();
            if(tableBodyDataList.size()== count){
            Toast.makeText(getContext(),"请先输入关键词再进行搜索！",Toast.LENGTH_SHORT).show();
            }else{
                tableHeader.removeAllViews();
                tableBody.removeAllViews();
                tableBodyList.clear();
                AsynchronousLoading();
            }return;
        }
        tableBody.removeAllViews();
        tableBodyList.clear();
        LoadData(searchWord);
    }

    synchronized private void showGoodsInfoPage(final int rowNumber){
        View view= LayoutInflater.from(getContext()).inflate(R.layout.dialog_show_goodsinfo_page, null);
        final TextView cancel =view.findViewById(R.id.showGoods_cancel);
        final TextView sure =view.findViewById(R.id.showGoods_sure);
        final EditText eText_name =view.findViewById(R.id.showGoods_name);
        final Spinner spinner_unit =view.findViewById(R.id.showGoods_unitList);
        final TextView tView_barcode=view.findViewById(R.id.showGoods_barcode);
        final EditText eText_price =view.findViewById(R.id.showGoods_price);
        final EditText eText_orig =view.findViewById(R.id.showGoods_orig);
        AlertDialog.Builder builder= new AlertDialog.Builder(getContext());
        final Dialog dialog= builder.create();
        dialog.show();
        dialog.getWindow().setContentView(view);
        dialog.setCancelable(false);
        final Goods goods = tableBodyDataList.get(rowNumber);
        eText_name.setText(goods.getName());
        eText_name.setSelection(goods.getName().length());
        tView_barcode.setText(goods.getBarcode());
        eText_price.setText(String.valueOf(goods.getPrice()));
        eText_orig.setText(String.valueOf(goods.getOrig()));
        //使editext可以唤起软键盘
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        final Goods goodsCheck = new Goods();//用来检测是否有改动操作
        spinner_unit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String item = spinner_unit.getSelectedItem().toString();
                goodsCheck.setUnit(item);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                goodsCheck.setUnit("个");
            }
        });
        //        String[] unitList = view.getResources().getStringArray(R.array.unitArray);
//        String units = Arrays.toString(unitList).replaceAll(", ","")
//                .replace("[请选择","").replace("]","");
        String units="个包件箱条罐听瓶排支袋副提盒桶斤卷";
        spinner_unit.setSelection(units.indexOf(goods.getUnit())+1,true);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                goodsCheck.setId(goods.getId());
                goodsCheck.setName(eText_name.getText().toString());
                goodsCheck.setBarcode(tView_barcode.getText().toString());
                String price = eText_price.getText().toString();
                if(!"".equals(price))
                    goodsCheck.setPrice(Float.valueOf(price));
                String orig = eText_orig.getText().toString();
                if(!"".equals(orig))
                    goodsCheck.setOrig(Float.valueOf(orig));
                /*没有改动直接关闭dialog*/
                if (goods.equals(goodsCheck)){dialog.dismiss();return;}
                HashMap<String, String> map = new HashMap<>();
                map.put("name",goodsCheck.getName());
                map.put("barcode",goodsCheck.getBarcode());
                map.put("unit",goodsCheck.getUnit());
                map.put("price",price);
                map.put("orig",orig);
                boolean ok = GoodsUtils.checkGoodsInfoForAction(getContext(),goodsCheck,map,GoodsUtils.DO_UPDATE);
                if (ok) {//表格实时更新数据显示
                    tableBodyList.get(rowNumber).get(0).setText(String.valueOf(rowNumber+1));
                    tableBodyList.get(rowNumber).get(1).setText(goodsCheck.getName());
                    tableBodyList.get(rowNumber).get(2).setText(goodsCheck.getUnit());
                    tableBodyList.get(rowNumber).get(3).setText(String.valueOf(goodsCheck.getPrice()));
                    tableBodyList.get(rowNumber).get(4).setText(String.valueOf(goodsCheck.getOrig()));
                    tableBodyDataList.set(rowNumber,goodsCheck);
                    dialog.dismiss();
                }
            }
        });
    }

}