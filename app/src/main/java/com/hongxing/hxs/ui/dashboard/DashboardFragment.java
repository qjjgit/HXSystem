package com.hongxing.hxs.ui.dashboard;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.hongxing.hxs.MainActivity;
import com.hongxing.hxs.R;
import com.hongxing.hxs.entity.Goods;
import com.hongxing.hxs.entity.PurchaseOrder;
import com.hongxing.hxs.service.CrudService;
import com.hongxing.hxs.utils.GoodsUtils;

import java.text.Collator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private short nowListPage=0x00;//0为商品信息列表，1为进货单列表
    private LinearLayout tableHeader;
    private LinearLayout tableBody;
    private String[] tableHeaderTexts={"序号","商品名称","单位","售价","进货价"};
    private ArrayList<ArrayList<MyTableTextView>> tableBodyList=new ArrayList<>();
    private ArrayList<Goods> goodsList;
    private ArrayList<PurchaseOrder> purOrderList;
    private DashboardViewModel dashboardViewModel;
    private Handler uiHandler;
    private String nowSearchWord;
    private String[] lastSort=null;

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
            final EditText searchTextV = root.findViewById(R.id.text_search);
            (root.findViewById(R.id.btn_search)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view){
                lastSort=null;
                nowSearchWord = (searchTextV.getText().toString()).replace(" ","");
                searchTextV.setText(nowSearchWord);
                searchTextV.setSelection(nowSearchWord.length());
                doSearch();
                }
            });
            final TextView btn = root.findViewById(R.id.btn_jumpPage);
            btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str1="转到进货单";
                String str2="输入名称或条码";
                if (nowListPage==0x00){
                    nowListPage=0x01;str1="转到商品列表";str2="输入供货商名称";
                }
                else nowListPage=0x00;
                searchTextV.setHint(str2);
                btn.setText(str1);
                nowSearchWord="";
                searchTextV.setText(nowSearchWord);
                tableHeader.removeAllViews();
                tableBody.removeAllViews();
//                if (nowListPage==0x00)initTableHeader();
//                if (purOrderList==null||purOrderList.isEmpty()||goodsList.isEmpty())
//                queryData();
//                loadData();
                AsynchronousLoading();
            }
            });
            AsynchronousLoading();
            }
        });
        return root;
    }
    @SuppressLint("HandlerLeak")
    private void AsynchronousLoading(){
        final ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("加载中...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
        uiHandler=new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case 0x03:{
                        if (nowListPage==0x00)
                        initTableHeader();
                        loadData();
                        break;
                    }
                    case 0x08:{
                        if (progressDialog.isShowing()) {
                            progressDialog.cancel();
                        }break;
                    }
                }
            }
        };
        new Thread(){
            @Override
            public void run() {
                queryData();
                // TODO: 2020/10/22 可以在线程里分多次查询数据，分多次追加数据进tableBody,避免一个时间内加载大量视图
//                try {
//                    sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                uiHandler.sendEmptyMessageDelayed(0x03,50);
            }
        }.start();
//        final Thread t = new Thread() {
//            public void run(){
//                boolean post = uiHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (nowListPage==0x00)
//                        initTableHeader();
//                        queryData();
//                        loadData();
//                    }
//                });
//                if (post){
//                    try {
//                        sleep(500);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    progressDialog.setProgress(10);
//                    progressDialog.cancel();
//                }
//            }
//        };
//        t.start();
    }

    private void initTableHeader(){
        LinearLayout relativeLayout=(LinearLayout) LayoutInflater.from(this.getContext()).inflate(R.layout.table,null);
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

    private void queryData(){
        final CrudService service = new CrudService(getContext());
        if (nowListPage==0x00){
            goodsList = service.findByPage(0, 999,nowSearchWord);
        }
        if (nowListPage==0x01){
            purOrderList=service.findPurOrderByWord(nowSearchWord);
            if (purOrderList.size()>1){
                final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                Collections.sort(purOrderList, new Comparator<PurchaseOrder>() {
                    @Override
                    public int compare(PurchaseOrder p1, PurchaseOrder p2) {
                        try {
                            long d1 = format.parse(p1.getDate()).getTime();
                            long d2 = format.parse(p2.getDate()).getTime();
                            if(d1<d2)return 1;
                            if(d1>d2)return -1;
                        } catch (ParseException e) {
                            Toast.makeText(getContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                        return 0;
                    }
                });
            }
        }service.close();
    }

    private void loadData(){
        final Context context = this.getContext();
        switch (nowListPage){
            case 0x00:{
                if(goodsList.size()<1){
                    TextView child = new TextView(getContext());
                    child.setTextSize(20);
                    child.setWidth(tableHeader.getWidth());
                    child.setPadding(0,20,0,0);
                    child.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    child.setText("没有查询到相关商品！");
                    tableBody.addView(child);
                    uiHandler.sendEmptyMessage(0x08);
                    return;
                }
                for(int i=0;i<goodsList.size();i++){
                    LinearLayout relativeLayout=(LinearLayout) LayoutInflater.from(context).inflate(R.layout.table,null);
                    int color = Color.parseColor("#ffffff");
                    if (i%2!=0) color= Color.parseColor("#eeeeee");
                    Goods goods = goodsList.get(i);
                    final MyTableTextView col1 = relativeLayout.findViewById(R.id.list_1_1);
                    col1.setBackgroundColor(color);
                    col1.setText(String.valueOf(goods.getId()));
                    final MyTableTextView col2 = relativeLayout.findViewById(R.id.list_1_2);
                    col2.setBackgroundColor(color);
                    col2.setText(goods.getName());
                    final MyTableTextView col3 = relativeLayout.findViewById(R.id.list_1_3);
                    col3.setBackgroundColor(color);
                    col3.setText(goods.getUnit());
                    final MyTableTextView col4 = relativeLayout.findViewById(R.id.list_1_4);
                    col4.setBackgroundColor(color);
                    col4.setText(String.valueOf(goods.getPrice()));
                    final MyTableTextView col5 = relativeLayout.findViewById(R.id.list_1_5);
                    col5.setBackgroundColor(color);
                    col5.setText(String.valueOf(goods.getOrig()));
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
                uiHandler.sendEmptyMessageDelayed(0x08,50);
                break;
            }
            case 0x01:{
                if(purOrderList.size()<1){
                    TextView child = new TextView(getContext());
                    child.setTextSize(20);
                    child.setWidth(tableHeader.getWidth());
                    child.setPadding(0,20,0,0);
                    child.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    child.setText("没有查询到相关进货单！");
                    tableBody.addView(child);
                    uiHandler.sendEmptyMessage(0x08);
                    return;
                }
                final float scale = context.getResources().getDisplayMetrics().density;
                for(int i=0;i<purOrderList.size();i++){
                    final LinearLayout row=(LinearLayout) LayoutInflater.from(context).inflate(R.layout.list_item,null);
//                    if (i%2!=0) row.setBackgroundResource(R.drawable.list_item_bg2);
                    final PurchaseOrder purchaseOrder = purOrderList.get(i);
                    Bitmap temp = BitmapFactory.decodeFile(purchaseOrder.getDataUri());
                    final Bitmap bitmap=MainActivity.centerSquareScaleBitmap(temp,100,scale);
                    ((ImageView)row.findViewById(R.id.pur_img_item)).setImageBitmap(bitmap);
                    ((TextView)row.findViewById(R.id.pur_supplier_item)).setText(purchaseOrder.getSupplier());
                    ((TextView)row.findViewById(R.id.pur_date_item)).setText(purchaseOrder.getDate());
                    row.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showPurOrderInfoPage(purchaseOrder);
                        }
                    });
                    final int rowIndex=i;
                    row.findViewById(R.id.pur_btn_item).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final String[] items = { "   添加/删除相关联的商品",
                                        "   修改当前进货单信息","   删除当前进货单",
                                        "                                                   返回"};
                            AlertDialog.Builder listDialog = new AlertDialog.Builder(context);
                            listDialog.setIcon(R.drawable.operating);
                            listDialog.setTitle("请选择");
                            listDialog.setCancelable(false);
                            listDialog.setItems(items, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which){
                                        /*增添进货单关联*/
                                        case 0:{
                                            AlertDialog.Builder builder= new AlertDialog.Builder(context);
                                            final Dialog dialogChoose= builder.create();
                                            dialogChoose.show();
                                            final View root= LayoutInflater.from(context).inflate(R.layout.fragment_dashboard, null);
                                            ((EditText)root.findViewById(R.id.text_search)).setWidth((int)(root.getWidth()*.15));
                                            root.findViewById(R.id.btn_add).setAlpha(0f);
                                            root.findViewById(R.id.btn_add).setEnabled(false);
                                            root.findViewById(R.id.btn_jumpPage).setAlpha(0f);
                                            root.findViewById(R.id.btn_jumpPage).setEnabled(false);
                                            Window dialogWindow = dialogChoose.getWindow();
                                            dialogWindow.setContentView(root);
                                            dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                                            DisplayMetrics metrics = new DisplayMetrics();
                                            dialogWindow.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
                                            WindowManager.LayoutParams attr = dialogWindow.getAttributes();
                                            attr.height=(int)(metrics.heightPixels*0.9f);
                                            attr.width=(int)(metrics.widthPixels*1f);
                                            attr.gravity= Gravity.FILL_HORIZONTAL;
                                            attr.alpha=1f;
                                            dialogWindow.setAttributes(attr);
                                            final LinearLayout header = root.findViewById(R.id.MyTableHeader);
                                            final LinearLayout tableBody=root.findViewById(R.id.MyTable);
                                            loadChoiceDialogItems(context,dialogChoose,header,tableBody,purchaseOrder,null);
                                            //查询
                                            final EditText et=root.findViewById(R.id.text_search);
                                            (root.findViewById(R.id.btn_search)).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    final String word = et.getText().toString().replaceAll(" ", "");
                                                    et.setText(word);et.setSelection(word.length());
                                                    header.removeAllViews();
                                                    tableBody.removeAllViews();
                                                    loadChoiceDialogItems(context,dialogChoose,header,tableBody,purchaseOrder,word);
                                                }
                                            });
                                            break;
                                        }
                                        /*修改进货单信息*/
                                        case 1:{
                                            AlertDialog.Builder builder= new AlertDialog.Builder(context);
                                            final Dialog dialogInput= builder.create();
                                            dialogInput.show();
                                            dialogInput.setCancelable(false);
                                            final View root= LayoutInflater.from(context).inflate(R.layout.suretoadd_purorder_page, null);
                                            ((TextView)root.findViewById(R.id.addPurOrder_title)).setText("修改进货单");
                                            ((ImageView)root.findViewById(R.id.img_addingPurOrder)).setImageBitmap(bitmap);
                                            final String supplier = purchaseOrder.getSupplier();
                                            ((EditText)root.findViewById(R.id.addPurOrder_supplier)).setText(supplier);
                                            final String date = purchaseOrder.getDate();
                                            ((Button)root.findViewById(R.id.addPurOrder_date)).setText(date);
                                            Window dialogWindow = dialogInput.getWindow();
                                            dialogWindow.setContentView(root);
                                            dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                                            (root.findViewById(R.id.addPurOrder_cancel)).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    dialogInput.dismiss();
                                                }
                                            });
                                            final Button et_date= root.findViewById(R.id.addPurOrder_date);
                                            et_date.setOnClickListener(new View.OnClickListener() {
                                                @RequiresApi(api = Build.VERSION_CODES.N)
                                                @Override
                                                public void onClick(View view) {
                                                    DatePickerDialog datePickerDialog = new DatePickerDialog(context, DatePickerDialog.THEME_HOLO_LIGHT);
                                                    DatePicker picker = datePickerDialog.getDatePicker();
                                                    picker.setMaxDate(new Date().getTime());
                                                    datePickerDialog.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                                                        @Override
                                                        public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                                                            et_date.setText(year +"-"+(month+1)+"-"+day);
                                                        }
                                                    });
                                                    datePickerDialog.show();
                                                }
                                            });
                                            final EditText et_supplier= root.findViewById(R.id.addPurOrder_supplier);
                                            (root.findViewById(R.id.addPurOrder_ok)).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    String update_date = et_date.getText().toString();
                                                    String update_supplier =et_supplier.getText().toString().replaceAll(" ","");
                                                    if (update_supplier.length()<1)update_supplier="未填写";
                                                    if (!supplier.equals(update_supplier)||!date.equals(update_date))
                                                    modifyPurOrderInfo(new PurchaseOrder(purchaseOrder.getId(),update_supplier,update_date),rowIndex);
                                                    dialogInput.dismiss();
                                                }
                                            });
                                            break;
                                        }
                                        /*删除进货单*/
                                        case 2:{
                                            final AlertDialog.Builder alterDiaglog = new AlertDialog.Builder(context);
                                            alterDiaglog.setIcon(R.drawable.error);//图标
                                            alterDiaglog.setTitle("系统提示");//文字
                                            alterDiaglog.setMessage("\n 确定删除当前进货单吗？");//提示消息
                                            //积极的选择
                                            alterDiaglog.setPositiveButton("取消", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {}
                                            });
                                            //消极的选择
                                            alterDiaglog.setNegativeButton("确定", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    CrudService service = new CrudService(context);
                                                    String msg="删除失败！";
                                                    if (service.deletePurOrder(purchaseOrder)){
                                                        msg="删除成功！";
                                                        tableBody.removeView(row);
                                                    }service.close();
                                                    Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                            alterDiaglog.show();
                                            break;
                                        }
                                        /*返回*/
                                        case 3:{}
                                    }
                                }
                            });
                            listDialog.show();
                        }
                    });
                    tableBody.addView(row);
                }
                uiHandler.sendEmptyMessageDelayed(0x08,50);
                break;
            }
        }
    }

    /*点击表头实现某一列排序*/
    private void setOnClick(MyTableTextView view){
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (goodsList.isEmpty())return;
                tableBody.removeAllViews();
                tableBodyList.clear();
                String sortAction;
                if(lastSort==null){
                    lastSort=new String[]{"",""};
                    sortAction="desc";
                }
                else if ("desc".equals(lastSort[1]))sortAction="asc";
                else sortAction="desc";
                final String s=sortAction;
                switch (v.getId()){
                    case R.id.list_1_1:
                        lastSort=new String[]{"id",sortAction};
                        Collections.sort(goodsList, new Comparator<Goods>() {
                            @Override
                            public int compare(Goods g1, Goods g2) {
                                if (!lastSort[0].equals("id")||s.equals("asc"))
                                return g1.getId()-g2.getId();
                                return g2.getId()-g1.getId();
                            }
                        });
                        loadData();
                        break;
                    case R.id.list_1_2:
                        lastSort=new String[]{"name",sortAction};
                        Collections.sort(goodsList, new Comparator<Goods>() {
                            @Override
                            public int compare(Goods g1, Goods g2) {
                                Collator c = Collator.getInstance(Locale.CHINA);
                                if (!lastSort[0].equals("name")||s.equals("asc"))
                                    return c.compare(g1.getName(),g2.getName());
                                return c.compare(g2.getName(),g1.getName());
                            }
                        });
                        loadData();
                        break;
                    case R.id.list_1_3:
                        lastSort=new String[]{"unit",sortAction};
                        Collections.sort(goodsList, new Comparator<Goods>() {
                            @Override
                            public int compare(Goods g1, Goods g2) {
                                Collator c = Collator.getInstance(Locale.CHINA);
                                if (!lastSort[0].equals("unit")||s.equals("asc"))
                                    return c.compare(g1.getUnit(),g2.getUnit());
                                return c.compare(g2.getUnit(),g1.getUnit());
                            }
                        });
                        loadData();
                        break;
                    case R.id.list_1_4:
                        lastSort=new String[]{"price",sortAction};
                        Collections.sort(goodsList, new Comparator<Goods>() {
                            @Override
                            public int compare(Goods g1, Goods g2) {
                                if (!lastSort[0].equals("price")||s.equals("asc"))
                                    return (int)(g1.getPrice()-g2.getPrice());
                                return (int)(g2.getPrice()-g1.getPrice());
                            }
                        });
                        loadData();
                        break;
                    case R.id.list_1_5:
                        lastSort=new String[]{"orig",sortAction};
                        Collections.sort(goodsList, new Comparator<Goods>() {
                            @Override
                            public int compare(Goods g1, Goods g2) {
                                if (!lastSort[0].equals("orig")||s.equals("asc"))
                                    return (int)(g1.getOrig()-g2.getOrig());
                                return (int)(g2.getOrig()-g1.getOrig());
                            }
                        });
                        loadData();
                        break;
                }
            }
        });
    }

    private void doSearch(){
        if(nowSearchWord.length()==1){
            Toast.makeText(getContext(),"至少输入2位字符进行搜索！",Toast.LENGTH_SHORT).show();
            return;
        }
        if ("".equals(nowSearchWord)){
            CrudService service = new CrudService(getContext());
            boolean count;
            if (nowListPage==0x00){
                count=goodsList.size()==service.getGoodsCount();
            }else count =purOrderList.size()==service.getPurOrderCount();
            service.close();
            if(count){
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
        queryData();
        loadData();
    }

    private ArrayList<ArrayList<String>> getChooseItemsAndDefaultSelectedList(String purId,String search){
        CrudService service = new CrudService(getContext());
        ArrayList<String> goodsNames = service.getGoodsNameListByPurOrderId(purId);
        ArrayList<String> allNameList = service.getDistinctGoodsNameList(search);
        service.close();
        if (goodsNames==null)goodsNames=new ArrayList<>();
        ArrayList<String> items = new ArrayList<>();
        for ( int j=0;j<goodsNames.size();j++){
            String item="["+(j+1)+"]      "+goodsNames.get(j);
            items.add(item);
        }
        int i=0;
        for (String name : allNameList){
            if (goodsNames.contains(name))continue;
            i++;
            String item="["+(goodsNames.size()+i)+"]      "+ name;
            items.add(item);
        }
        ArrayList<ArrayList<String>> list=new ArrayList<>();
        list.add(items);
        list.add(goodsNames);
        return list;
    }

    private void loadChoiceDialogItems(final Context context,final Dialog dialogChoose,final LinearLayout header,final LinearLayout tableBody, final PurchaseOrder purchaseOrder,String word){
        ArrayList<ArrayList<String>> itemList = getChooseItemsAndDefaultSelectedList(purchaseOrder.getId(),word);
        final String[] items= itemList.get(0).toArray(new String[0]);
        final ArrayList<String> defaultSelected = itemList.get(1);
        final ArrayList<String> goodsNameChoices = new ArrayList<>();
        for (int i=0;i<items.length;i++) {
            if (defaultSelected.isEmpty()&&i==0){
                TextView tv1 = new TextView(getContext());
                String str="新增请勾选：";
                if (defaultSelected.size()==items.length)str="\n         没有查询到相关商品";
                tv1.setText(str);
                tv1.setTextColor(Color.rgb(216,27,96));
                tableBody.addView(tv1);
            }
            if (i==0&&defaultSelected.size()!=0){
                try {
                TextView tv2 = new TextView(getContext());
                tv2.setText("已关联的：");
                tv2.setTextColor(Color.rgb(216,27,96));
                tableBody.addView(tv2);
                if (defaultSelected.size()<1){
                    tv2.setText("           无");
                    tv2.setTextColor(Color.BLACK);
                    tableBody.addView(tv2);
                }
                }catch (Exception e){
                    Toast.makeText(getContext(),e.getMessage(),Toast.LENGTH_LONG).show();
                }
            }
            String itemStr=items[i];
            final CheckBox item = new CheckBox(context);
            item.setText(itemStr);
            final String goodsName = itemStr.substring(itemStr.lastIndexOf(" ")+1);
            if (defaultSelected.contains(goodsName)){
                goodsNameChoices.add(goodsName);
                item.setChecked(true);}
            item.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    if (isChecked) {
                        goodsNameChoices.add(goodsName);
                    } else {
                        goodsNameChoices.remove(goodsName);
                    }
                }
            });
            tableBody.addView(item);
            if (i==defaultSelected.size()-1){
                LinearLayout v = new LinearLayout(context);
                v.setMinimumHeight(2);
                WindowManager.LayoutParams attr = dialogChoose.getWindow().getAttributes();
                v.setMinimumWidth((int)(attr.width*0.91f));
                v.setBackgroundColor(Color.BLACK);
                tableBody.addView(v);
                TextView tv = new TextView(getContext());
                String str="新增请勾选：";
                if (defaultSelected.size()==items.length)str="\n         没有查询到相关商品";
                tv.setText(str);
                tv.setTextColor(Color.rgb(216,27,96));
                tableBody.addView(tv);
            }
        }
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.END);
        header.setPadding(0, 1, 15, 1);
        TextView textV = new TextView(context);
        textV.setText("请勾选要关联的商品并点击提交    ");
        header.addView(textV);
        Button button = new Button(context);
        button.setText("提交");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!goodsNameChoices.equals(defaultSelected)){
                    CrudService service = new CrudService(context);
                    String msg="关联失败！";
                    if (service.bindingGoods2PurOrder(goodsNameChoices,purchaseOrder.getId())) {
                        msg="关联成功！";
                    }
                    service.close();
                    Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
                }
                dialogChoose.dismiss();
            }
        });
        header.addView(button);
    }

    private void modifyPurOrderInfo(PurchaseOrder purO,int rowIndex){
        Context context = getContext();
        CrudService service = new CrudService(context);
        service.updatePurOrder(purO);service.close();
        purOrderList.set(rowIndex,purO);
        ((TextView)tableBody.getChildAt(rowIndex).findViewById(R.id.pur_supplier_item)).setText(purO.getSupplier());
        ((TextView)tableBody.getChildAt(rowIndex).findViewById(R.id.pur_date_item)).setText(purO.getDate());
        Toast.makeText(context,"更新成功！",Toast.LENGTH_SHORT).show();
        loadData();// TODO: 2020/10/23 看看卡不卡
    }

    synchronized private void showGoodsInfoPage(final int rowNumber){
        AlertDialog.Builder builder= new AlertDialog.Builder(getContext());
        final Dialog dialog= builder.create();
        dialog.show();
        View view= LayoutInflater.from(getContext()).inflate(R.layout.dialog_show_goodsinfo_page, null);
        dialog.getWindow().setContentView(view);
        dialog.setCancelable(false);
        final TextView cancel =view.findViewById(R.id.showGoods_cancel);
        final TextView sure =view.findViewById(R.id.showGoods_sure);
        final EditText eText_name =view.findViewById(R.id.showGoods_name);
        final Spinner spinner_unit =view.findViewById(R.id.showGoods_unitList);
        final TextView tView_barcode=view.findViewById(R.id.showGoods_barcode);
        final EditText eText_price =view.findViewById(R.id.showGoods_price);
        final EditText eText_orig =view.findViewById(R.id.showGoods_orig);
        final Goods goods = goodsList.get(rowNumber);
        MainActivity.goods=goods;
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
                if (price.startsWith("."))price="0"+price;
                if (price.endsWith("."))price=price+"0";
                if(!"".equals(price))
                    goodsCheck.setPrice(Float.valueOf(price));
                String orig = eText_orig.getText().toString();
                if (orig.startsWith("."))orig="0"+orig;
                if (orig.endsWith("."))orig=orig+"0";
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
                    goodsList.set(rowNumber,goodsCheck);
                    dialog.dismiss();
                }
            }
        });
    }

    synchronized private void showPurOrderInfoPage(PurchaseOrder purchaseOrder){
        Context context = getContext();
        AlertDialog.Builder builder= new AlertDialog.Builder(context,R.style.Dialog_Fullscreen);
        final Dialog dialog= builder.create();
        dialog.show();
        View root= LayoutInflater.from(context).inflate(R.layout.show_bitmap_full, null);
        dialog.getWindow().setContentView(root);
        ImageView imgV=root.findViewById(R.id.img_fullscreen);
        String uri= purchaseOrder.getDataUri();
        Bitmap bitmap = BitmapFactory.decodeFile(uri);
        imgV.setImageBitmap(bitmap);
        imgV.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }
}