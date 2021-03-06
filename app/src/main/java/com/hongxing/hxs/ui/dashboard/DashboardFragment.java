package com.hongxing.hxs.ui.dashboard;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.hongxing.hxs.MainActivity;
import com.hongxing.hxs.R;
import com.hongxing.hxs.entity.Goods;
import com.hongxing.hxs.entity.PurchaseOrder;
import com.hongxing.hxs.service.CrudService;
import com.hongxing.hxs.ui.dialog.MyDialog;
import com.hongxing.hxs.utils.BitmapUtil;
import com.hongxing.hxs.utils.CommonUtils;
import com.hongxing.hxs.utils.GoodsUtils;
import com.hongxing.hxs.utils.ScreenUtil;
import com.hongxing.hxs.utils.StatusCode;
import com.hongxing.hxs.utils.ToastUtil;
import com.hongxing.hxs.utils.http.HttpUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Collator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

// TODO: 2020/11/15 待优化：进货单列表应该加入分页加载，避免总数据过多导致加载过慢，影响体验
public class DashboardFragment extends Fragment {

    private short nowListPage=0x00;//0为商品信息列表，1为进货单列表
    private LinearLayout tableHeader;
    private LinearLayout tableBody;
    private String[] tableHeaderTexts={"序号","商品名称","单位","售价","进货价"};
    private ArrayList<ArrayList<MyTableTextView>> tableBodyList=new ArrayList<>();
    private ArrayList<Goods> goodsList;
    private ArrayList<PurchaseOrder> purOrderList;
    private LinearLayout purListView;
    private ArrayList<Bitmap> compImgs=new ArrayList<>();
    private DashboardViewModel dashboardViewModel;
    private Handler uiHandler=null;
    private ProgressDialog progressDialog;
    private String nowSearchWord;
    private String[] lastSort=null;
    private Context mContext;

    public DashboardFragment() {}
    public DashboardFragment(Context context) {
        mContext=context;
        initSomething(null);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                ViewModelProviders.of(this).get(DashboardViewModel.class);
        final View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        initSomething(root);
        dashboardViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
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
            final TextView btnAdd=root.findViewById(R.id.btn_add);
            btnAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (nowListPage == 0x00) MyDialog.showAddGoodsPage(getContext());
                    else useCameraForAddPurOrder(getContext());
                }
            });
            final TextView btnJump = root.findViewById(R.id.btn_jumpPage);
            btnJump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str1="转到进货单";
                String str2="输入名称或条码";
                String str3="添加商品";
                if (nowListPage==0x00){
                    nowListPage=0x01;str1="转到商品列表";str2="输入供货商名称";str3="添加货单";
                }
                else nowListPage=0x00;
                searchTextV.setHint(str2);
                btnJump.setText(str1);
                btnAdd.setText(str3);
                nowSearchWord="";
                searchTextV.setText(nowSearchWord);
                tableHeader.removeAllViews();
                tableBody.removeAllViews();
                AsynchronousLoading();
            }
            });
            AsynchronousLoading();
            }
        });
        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != MainActivity.RESULT_OK ) {
            if (requestCode== StatusCode.REQUEST_CODE_SHOOT){
                MainActivity.showPIC.delete();
            }
            return;
        }
        //拍照 进货单结果
        if (requestCode == StatusCode.REQUEST_CODE_SHOOT){
            final Context context = getContext();
            View view= LayoutInflater.from(context).inflate(R.layout.suretoadd_purorder_page, null);
            final Bitmap bitmap_orig = BitmapFactory.decodeFile(MainActivity.showPIC.getPath());
            final Bitmap bitmap_comp=BitmapUtil.centerSquareScaleBitmap(bitmap_orig,150);
            ImageView imgView=view.findViewById(R.id.img_addingPurOrder);
            imgView.setImageBitmap(bitmap_comp);
            AlertDialog.Builder builder= new AlertDialog.Builder(context);
            final Dialog dialog= builder.create();
            dialog.show();
            dialog.getWindow().setContentView(view);
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            dialog.setCancelable(false);
            (view.findViewById(R.id.addPurOrder_cancel)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    MainActivity.showPIC.delete();
                }
            });
            final Button addPurOrder_date= view.findViewById(R.id.addPurOrder_date);
            addPurOrder_date.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onClick(View view) {
                    DatePickerDialog datePickerDialog = new DatePickerDialog(context, DatePickerDialog.THEME_HOLO_LIGHT);
                    datePickerDialog.getDatePicker().setMaxDate(new Date().getTime());
                    datePickerDialog.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                            addPurOrder_date.setText(year +"-"+(month+1)+"-"+day);
                        }
                    });
                    datePickerDialog.show();
                }
            });
            final EditText addPurOrder_supplier= view.findViewById(R.id.addPurOrder_supplier);
            (view.findViewById(R.id.addPurOrder_ok)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String strDate = addPurOrder_date.getText().toString();
                    if ("请选择".equals(strDate)){
                        ToastUtil.showShortToast("请先选择进货单的日期！");
                        return;
                    }
                    String supplier =addPurOrder_supplier.getText().toString().replaceAll(" ","");
                    if (supplier.length()<1)supplier="未填写";
                    sureToAdd_purOrder(context,strDate,supplier,bitmap_comp);
                    dialog.dismiss();
                }
            });
        }
    }

    //确定添加进货单
    private void sureToAdd_purOrder(Context context, String strDate, String supplier,Bitmap compBitmap){
        PurchaseOrder purchaseOrder = new PurchaseOrder(UUID.randomUUID().toString(),supplier,strDate);
        CrudService service = new CrudService(context);
        service.savePurchaseOrder(MainActivity.goods,purchaseOrder);
        service.close();
        final LinearLayout row = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.list_item,null);
        ((ImageView)row.findViewById(R.id.pur_img_item)).setImageBitmap(compBitmap);
        ((TextView) row.findViewById(R.id.pur_supplier_item)).setText(supplier);
        ((TextView) row.findViewById(R.id.pur_date_item)).setText(strDate);
        if (purListView!=null){
            setPurItemOnClickEvent(context,row,purchaseOrder,purListView.getChildCount(),StatusCode.IN_PURLIST_VIEW);
            purListView.addView(row);
        }else{
            setPurItemOnClickEvent(context,row,purchaseOrder,tableBody.getChildCount(),StatusCode.IN_TABLE_BODY);
            tableBody.addView(row);
        }
        ToastUtil.showShortToast("进货单添加成功!");
        new Thread(()->{
            String imgPath=purchaseOrder.getCachePath();
            try {
                FileOutputStream fos = new FileOutputStream(imgPath);
                compBitmap.compress(Bitmap.CompressFormat.JPEG,20,fos);
                fos.flush();fos.close();
                //原图较大,压缩原图后保存到磁盘中
                Bitmap bitmap = BitmapFactory.decodeFile(MainActivity.showPIC.getPath());
                File file2 = new File(purchaseOrder.getDataUri());
                FileOutputStream outputStream = new FileOutputStream(file2);
                bitmap.compress(Bitmap.CompressFormat.JPEG,50,outputStream);
                outputStream.flush();outputStream.close();bitmap.recycle();
                MainActivity.showPIC.delete();
                MainActivity.showPIC=file2;
                //上传到服务器
                HttpUtils.uploadImg(file2, new HttpUtils.Listener() {
                    @Override
                    public void startFileTransfer() { }
                    @Override
                    public void success(String response) {
                        uiHandler.sendEmptyMessage(0x11);
                    }
                    @Override
                    public void error(Exception e) {
                        if (e.getMessage().startsWith("responseCode"))
                            uiHandler.sendEmptyMessage(0x12);
                        else
                        HttpUtils.sendErrorLog(e.getMessage(), new HttpUtils.Listener() {
                            @Override
                            public void startFileTransfer() { }
                            @Override
                            public void success(String response) {
                                uiHandler.sendEmptyMessage(0x09);
                            }
                            @Override
                            public void error(Exception e) {
                                uiHandler.sendEmptyMessage(0x10);
                            }
                        });
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @SuppressLint("HandlerLeak")
    private void initSomething(View root){
        if (root!=null){
            tableHeader=root.findViewById(R.id.MyTableHeader);
            tableBody=root.findViewById(R.id.MyTable);
        }
        if (progressDialog==null) {
            progressDialog = new ProgressDialog(mContext==null?getContext():mContext);
            progressDialog.setMessage("加载中...");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
        }
        if (uiHandler==null)
            uiHandler=new Handler(){
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    LinearLayout table = (LinearLayout) msg.obj;
                    switch (msg.what){
                        case 0x03:{
                            if (nowListPage==0x00)
                                initTableHeader();
                            loadData();
                            break;
                        }
                        case 0x07:{
                            Bundle data = msg.getData();
                            int index = data.getInt("index");
                            Bitmap bitmap = compImgs.get(index);
                            try {
                                ((ImageView)table.getChildAt(index).findViewById(R.id.pur_img_item)).setImageBitmap(bitmap);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            break;
                        }
                        case 0x08:{
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }break;
                        }
                        case 0x09:{
                            Toast.makeText(getContext(),"错误日志已发送到服务器!", Toast.LENGTH_LONG).show();
                        }
                        case 0x10:{
                            Toast.makeText(getContext(),"错误日志发送失败!", Toast.LENGTH_LONG).show();
                        }
                        case 0x11:{
                            Toast.makeText(getContext(),"进货单已上传到数据中心!", Toast.LENGTH_SHORT).show();
                        }
                        case 0x12:{
                            Toast.makeText(getContext(),"服务器维护中,进货单未上传!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            };
    }

    private void AsynchronousLoading(){
        progressDialog.show();
        new Thread(){
            @Override
            public void run() {
                queryData();
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
//                    progressDialog.cancel();
//                }
//            }
//        };
//        t.start();
    }

    //加载表头
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

    //查询商品or进货单数据
    private void queryData(){
        final CrudService service = new CrudService(getContext());
        if (nowListPage==0x00){
            goodsList = service.findByPage(0, 999,nowSearchWord);
        }
        if (nowListPage==0x01){
            purOrderList=service.findPurOrderByWord(nowSearchWord);
            if (purOrderList.size()>1){
                purListSortByTime(purOrderList);
            }
        }service.close();
    }

    //按时间倒序 排进货单
    private void purListSortByTime(ArrayList<PurchaseOrder> purOrderList){
        if (purOrderList==null||purOrderList.size()<2)return;
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd",Locale.CHINA);
        Collections.sort(purOrderList, new Comparator<PurchaseOrder>() {
            @Override
            public int compare(PurchaseOrder p1, PurchaseOrder p2) {
                try {
                    long d1 = format.parse(p1.getDate()).getTime();
                    long d2 = format.parse(p2.getDate()).getTime();
                    if(d1<d2)return 1;
                    if(d1>d2)return -1;
                } catch (ParseException e) {
                    ToastUtil.showShortToast(e.getMessage());
                }
                return 0;
            }
        });
    }

    //加载商品or进货单数据到table
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
                for(int i=0;i<purOrderList.size();i++){
                    final LinearLayout row=(LinearLayout) LayoutInflater.from(context).inflate(R.layout.list_item,null);
                    final PurchaseOrder purchaseOrder = purOrderList.get(i);
                    ((TextView)row.findViewById(R.id.pur_supplier_item)).setText(purchaseOrder.getSupplier());
                    ((TextView)row.findViewById(R.id.pur_date_item)).setText(purchaseOrder.getDate());
                    setPurItemOnClickEvent(context,row,purchaseOrder,i,StatusCode.IN_TABLE_BODY);
                    tableBody.addView(row);
                }
                asynchronousLoadPurImgs(context,tableBody,purOrderList);
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

    //查询
    private void doSearch(){
        if(nowSearchWord.length()==1){
            ToastUtil.showShortToast("至少输入2位字符进行搜索！");
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
                ToastUtil.showShortToast("请先输入关键词再进行搜索！");
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

    //修改进货单信息
    private void modifyPurOrderInfo(PurchaseOrder purO,int rowIndex){
        Context context = getContext();
        CrudService service = new CrudService(context);
        service.updatePurOrder(purO);service.close();
        purOrderList.set(rowIndex,purO);
        tableBody.removeAllViews();
        loadData();
        ToastUtil.showShortToast("修改成功！");
    }

    //异步加载进货单略缩图
    private void asynchronousLoadPurImgs(Context context, final LinearLayout table, final ArrayList<PurchaseOrder> purOrderList){
        final String cachePath = CommonUtils.getDiskCachePath();
        File[] files = new File(cachePath).listFiles();
        final ArrayList<String> cacheList=new ArrayList<>();
        if (files!=null){
            for (File file : files) {
                cacheList.add(file.getAbsolutePath());
            }
        }
        compImgs.clear();
        new Thread(){
            @Override
            public void run() {
                for (int i = 0; i < purOrderList.size(); i++) {
                    PurchaseOrder pur = purOrderList.get(i);
                    if (pur==null){compImgs.add(null);continue;}
                    String imgPath = pur.getCachePath();
                    Bitmap bitmap=null;
                    if (cacheList.contains(imgPath)){
                        bitmap=BitmapFactory.decodeFile(imgPath);
                    }else{
                        File file = new File(pur.getDataUri());
                        if (!file.exists()){
                            HttpURLConnection connection=null;
                            try {//从服务器获取略缩图
                                String form="fileName="+pur.getFileName();
                                connection = HttpUtils.getDoGetConnection(
                                        new URL(CommonUtils.SERVERADDRESS + "/getThumbnail?" + form));
                                connection.connect();
                                if (connection.getResponseCode()==200)
                                    bitmap=BitmapFactory.decodeStream(connection.getInputStream());
                            } catch (Exception e) {
                                System.out.println("服务中心维护中!");
                                e.printStackTrace();
                            }finally { if (connection!=null)connection.disconnect(); }
                        }else{
                            Bitmap temp = BitmapFactory.decodeFile(pur.getDataUri());
                            bitmap= BitmapUtil.centerSquareScaleBitmap(temp,150);
                        }
                    }
                    if (bitmap==null)
                        bitmap=BitmapFactory.decodeResource(getResources(),R.drawable.img_load_failed);
                    else {
                        try {//保存到磁盘中
                            FileOutputStream fos = new FileOutputStream(imgPath);
                            bitmap.compress(Bitmap.CompressFormat.JPEG,20,fos);
                            fos.flush();fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    compImgs.add(bitmap);
                    Bundle bundle = new Bundle();
                    bundle.putInt("index",i);
                    Message msg = new Message();
                    msg.what = 0x07;
                    msg.obj=table;
                    msg.setData(bundle);
                    uiHandler.sendMessage(msg);
                }
                uiHandler.sendEmptyMessage(0x08);
            }
        }.start();
    }

    //显示商品信息
    synchronized private void showGoodsInfoPage(final int rowNumber){
        final Context context = getContext();
        AlertDialog.Builder builder= new AlertDialog.Builder(context);
        final Dialog dialog= builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                MainActivity.goods=null;
            }
        });
        dialog.show();
        View view= LayoutInflater.from(context).inflate(R.layout.dialog_show_goodsinfo_page, null);
        dialog.getWindow().setContentView(view);
//        dialog.setCancelable(false);
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
        view.findViewById(R.id.showGoods_PurchaseOrder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPurchaseOrderPage(context);
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
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
                boolean ok = GoodsUtils.checkGoodsInfoForAction(context,goodsCheck,map,GoodsUtils.DO_UPDATE);
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

    //全屏显示进货单图片
    @SuppressLint("ClickableViewAccessibility")
    synchronized private void fullScreenShowPurOrder(PurchaseOrder purchaseOrder){
        final Context context = mContext==null?getContext():mContext;
        AlertDialog.Builder builder= new AlertDialog.Builder(context,R.style.Dialog_Fullscreen);
        final Dialog dialog= builder.create();
        dialog.show();
        View root= LayoutInflater.from(context).inflate(R.layout.show_bitmap_full, null);
        dialog.getWindow().setContentView(root);
        final ImageView imgV=root.findViewById(R.id.img_fullscreen);
        String uri= purchaseOrder.getDataUri();
        AtomicReference<Bitmap> origBM = new AtomicReference<>();
        AtomicReference<Bitmap> pngBM = new AtomicReference<>();
        final DisplayMetrics metrics = ScreenUtil.getScreenSize(context);
        dialog.setOnCancelListener(m->{
            origBM.set(null);pngBM.set(null);
            System.gc();
        });
        @SuppressLint("HandlerLeak")
        Handler handler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what==0x00){
                    Bitmap bitmap = pngBM.get();
                    imgV.setImageBitmap(bitmap);
                    Bitmap magnified = BitmapUtil.proportionalScaleBitmap(origBM.get(), (int) (metrics.widthPixels * 1.62));
                    final BitmapUtil.CheckZoom check = new BitmapUtil.CheckZoom(false);
                    imgV.setOnTouchListener(new OnDoubleClickListener(() -> {
                        if (check.isMagnified())
                            imgV.setImageBitmap(bitmap);
                        else
                            imgV.setImageBitmap(magnified);
                        check.setMagnified(!check.isMagnified());
                    }));
                }
                if (msg.what==0x01){
                    imgV.setImageResource(R.drawable.img_load_failed);
                    Toast.makeText(context,"数据中心维护中!\n请稍后再试!", Toast.LENGTH_LONG).show();
                }
                if (msg.what==0x02){
                    imgV.setImageResource(R.drawable.img_load_failed);
                    Toast.makeText(context,"数据中心没有对应进货单!", Toast.LENGTH_LONG).show();
                }
            }
        };
        new Thread(()->{
            //磁盘中有原图的temp
            if (new File(uri).exists()){
                origBM.set(BitmapFactory.decodeFile(uri));
                pngBM.set(BitmapUtil.proportionalScaleBitmap(origBM.get(),metrics.widthPixels));
                handler.sendEmptyMessage(0x00);
            }else{//从服务器获取原图
                HttpURLConnection connection=null;
                try {
                    String form="fileName="+purchaseOrder.getFileName();
                    connection = HttpUtils.getDoGetConnection(
                            new URL(CommonUtils.SERVERADDRESS + "/getImg?" + form));
                    connection.connect();
                    if (connection.getResponseCode()==200){
                        origBM.set(BitmapFactory.decodeStream(connection.getInputStream()));
                        pngBM.set(BitmapUtil.proportionalScaleBitmap(origBM.get(),metrics.widthPixels));
                    }
//                    origBM.set(BitmapFactory.decodeStream(
//                            new URL(CommonUtils.SERVERADDRESS+"/getImg?"+form).openStream()));
                } catch (IOException e) {
                    handler.sendEmptyMessage(0x01);
                    e.printStackTrace();
                    return;
                }finally {if (connection!=null)connection.disconnect();}
                if (origBM.get()==null||pngBM.get()==null){
                    handler.sendEmptyMessage(0x02);
                    return;
                }
                handler.sendEmptyMessage(0x00);
                //存入磁盘中
                new Thread(()->{
                    try {
                        FileOutputStream fos = new FileOutputStream(purchaseOrder.getDataUri());
                        origBM.get().compress(Bitmap.CompressFormat.JPEG,50,fos);
                        fos.flush();fos.close();
                    }catch (Exception e){e.printStackTrace();}
                }).start();
            }
        }).start();
    }

    //查看进货单列表
    public void showPurchaseOrderPage(final Context context){
        View root;
        try {
            root= LayoutInflater.from(context).inflate(R.layout.purchaseorder_page, null);
        }catch (Exception e){e.printStackTrace();return;}
        AlertDialog.Builder builder= new AlertDialog.Builder(context,R.style.Dialog_Fullscreen);
        final Dialog dialog= builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                purListView=null;
            }
        });
        dialog.show();
        dialog.getWindow().setContentView(root);
        //添加进货单
        root.findViewById(R.id.btn_addPurOrder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                useCameraForAddPurOrder(context);
            }
        });
        //关联现有进货单
        root.findViewById(R.id.btn_linkPurOrder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMultipleChoiceDialog(context,MainActivity.goods);
            }
        });
        purListView=root.findViewById(R.id.purList_table);
        CrudService service = new CrudService(context);
        final ArrayList<PurchaseOrder> purOrderList = service.getPurOrderListByGoodsId(MainActivity.goods.getId());
        service.close();
        if (purOrderList.size()<1){
            TextView tV = new TextView(context);
            tV.setWidth(ScreenUtil.getScreenSize(context).widthPixels);
            tV.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tV.setText("该商品未添加进货单！");
            tV.setTextSize(24);
            purListView.addView(tV);
            return;
        }
        for (int i = 0; i < purOrderList.size(); i++) {
            final PurchaseOrder pur = purOrderList.get(i);
            final LinearLayout row = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.list_item, null);
            ((TextView) row.findViewById(R.id.pur_supplier_item)).setText(pur.getSupplier());
            ((TextView) row.findViewById(R.id.pur_date_item)).setText(pur.getDate());
            setPurItemOnClickEvent(context,row,pur,i,StatusCode.IN_PURLIST_VIEW);
            purListView.addView(row);
        }
        asynchronousLoadPurImgs(context,purListView,purOrderList);
    }

    //给进货单item添加onClick事件
    private void setPurItemOnClickEvent(final Context context,final LinearLayout row,final PurchaseOrder pur,final int rowIndex,final int where){
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fullScreenShowPurOrder(pur);
            }
        });
        row.findViewById(R.id.pur_btn_item).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder listDialog = new AlertDialog.Builder(context);
                listDialog.setIcon(R.drawable.operating);
                listDialog.setTitle("请选择");
                listDialog.setCancelable(false);
                String[] items;
                DialogInterface.OnClickListener listener;
                if (where==StatusCode.IN_PURLIST_VIEW ){
                    items = new String[]{"   修改当前进货单信息", "   取消关联当前进货单",
                            "                                                   返回"};
                    listener=new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                /*修改进货单信息*/
                                case 0:{
                                    AlertDialog.Builder builder= new AlertDialog.Builder(context);
                                    final Dialog dialogInput= builder.create();
                                    dialogInput.show();
                                    dialogInput.setCancelable(false);
                                    final View root= LayoutInflater.from(context).inflate(R.layout.suretoadd_purorder_page, null);
                                    ((TextView)root.findViewById(R.id.addPurOrder_title)).setText("修改进货单");
                                    Bitmap bitmap = compImgs.get(rowIndex);
                                    ((ImageView)root.findViewById(R.id.img_addingPurOrder)).setImageBitmap(bitmap);
                                    final String supplier = pur.getSupplier();
                                    ((EditText)root.findViewById(R.id.addPurOrder_supplier)).setText(supplier);
                                    final String date = pur.getDate();
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
                                                modifyPurOrderInfo(new PurchaseOrder(pur.getId(),update_supplier,update_date,pur.getDataUri()),rowIndex);
                                            dialogInput.dismiss();
                                        }
                                    });
                                    break;
                                }
                                /*取消关联当前进货单*/
                                case 1:{
                                    final AlertDialog.Builder alterDiaglog = new AlertDialog.Builder(context);
                                    alterDiaglog.setIcon(R.drawable.error);//图标
                                    alterDiaglog.setTitle("系统提示");//文字
                                    alterDiaglog.setMessage("\n 确定取消关联当前进货单吗？");//提示消息
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
                                            String msg="操作失败！";
                                            if (service.unlinkPurOrder(MainActivity.goods,pur)){
                                                msg="操作成功！";
                                                purListView.removeView(row);
                                            }service.close();
                                            ToastUtil.showShortToast(msg);
                                        }
                                    });
                                    alterDiaglog.show();
                                    break;
                                }
                                /*返回*/
                                case 2:{}
                            }
                        }
                    };
                }
                else {
                    items = new String[]{"   添加/删除相关联的商品",
                            "   修改当前进货单信息", "   删除当前进货单",
                            "                                                   返回"};
                    listener= new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                /*增添进货单关联*/
                                case 0:{
                                    showMultipleChoiceDialog(context,pur);
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
                                    Bitmap bitmap = compImgs.get(rowIndex);
                                    ((ImageView)root.findViewById(R.id.img_addingPurOrder)).setImageBitmap(bitmap);
                                    final String supplier = pur.getSupplier();
                                    ((EditText)root.findViewById(R.id.addPurOrder_supplier)).setText(supplier);
                                    final String date = pur.getDate();
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
                                                modifyPurOrderInfo(new PurchaseOrder(pur.getId(),update_supplier,update_date,pur.getDataUri()),rowIndex);
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
                                            if (service.deletePurOrder(pur)){
                                                msg="删除成功！";
                                                tableBody.removeView(row);
                                            }service.close();
                                            ToastUtil.showShortToast(msg);
                                        }
                                    });
                                    alterDiaglog.show();
                                    break;
                                }
                                /*返回*/
                                case 3:{}
                            }
                        }
                    };
                }
                listDialog.setItems(items,listener);
                listDialog.show();
            }
        });
    }
    
    /*
    *   显示 商品 或 进货单 多选框
    *   obj为 Goods 或 PurOrder
    */
    private void showMultipleChoiceDialog(final Context context,final Object obj){
        AlertDialog.Builder builder= new AlertDialog.Builder(context);
        final Dialog dialogChoose= builder.create();
        dialogChoose.show();
        final View root= LayoutInflater.from(context).inflate(R.layout.fragment_dashboard, null);
        root.findViewById(R.id.btn_add).setAlpha(0f);
        root.findViewById(R.id.btn_add).setEnabled(false);
        root.findViewById(R.id.btn_jumpPage).setAlpha(0f);
        root.findViewById(R.id.btn_jumpPage).setEnabled(false);
        final Window dialogWindow = dialogChoose.getWindow();
        dialogWindow.setContentView(root);
        dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        DisplayMetrics metrics = ScreenUtil.getScreenSize(context);
        WindowManager.LayoutParams attr = dialogWindow.getAttributes();
        attr.height=(int)(metrics.heightPixels*.9f);
        attr.width=(int)(metrics.widthPixels*1f);
        attr.alpha=1f;
        dialogWindow.setAttributes(attr);
        final LinearLayout header = root.findViewById(R.id.MyTableHeader);
        final LinearLayout tableBody=root.findViewById(R.id.MyTable);
        if (obj instanceof PurchaseOrder){
            ((EditText)root.findViewById(R.id.text_search)).setHint("输入商品名称");
            loadChoiceDialogItems(context,dialogChoose,header,tableBody,(PurchaseOrder)obj,null);
        }
        else{
            ((EditText)root.findViewById(R.id.text_search)).setHint("输入供货商名称");
            loadPurChoiceDialogItems(context,dialogChoose,header,tableBody,(Goods)obj,null);}
        //查询
        final EditText et=root.findViewById(R.id.text_search);
        (root.findViewById(R.id.btn_search)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String word = et.getText().toString().replaceAll(" ", "");
                et.setText(word);et.setSelection(word.length());
                header.removeAllViews();
                tableBody.removeAllViews();
                if (obj instanceof PurchaseOrder)
                loadChoiceDialogItems(context,dialogChoose,header,tableBody,(PurchaseOrder)obj,word);
                else loadPurChoiceDialogItems(context,dialogChoose,header,tableBody,(Goods)obj,word);
            }
        });
    }

    //获取商品多选框items的内容 & 默认选中的items
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

    //加载商品多选框的items & btn
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
                    ToastUtil.showLongToast(e.getMessage());
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
                    ToastUtil.showShortToast(msg);
                }
                dialogChoose.dismiss();
            }
        });
        header.addView(button);
    }

    //加载进货单多选框的items & CheckBox
    private void loadPurChoiceDialogItems(final Context context,final Dialog dialog,final LinearLayout header,final LinearLayout tableBody,final Goods goods,String word){
        progressDialog.show();
        CrudService service = new CrudService(context);
        final ArrayList<PurchaseOrder> defaultSelected = service.getPurOrderListByGoodsId(goods.getId());
        final ArrayList<PurchaseOrder> purOrderList = service.findPurOrderByWord(word);service.close();
        purListSortByTime(purOrderList);
        boolean isEmpty=purOrderList.isEmpty();
        boolean sizeEqual=defaultSelected.size()==purOrderList.size();
        final ArrayList<PurchaseOrder> purChoices=new ArrayList<>();
        if (defaultSelected.size()>0){
            TextView t = new TextView(context);
            t.setText("已关联的：");
            t.setTextColor(Color.rgb(216,27,96));
            tableBody.addView(t);
            for (int i = 0; i < defaultSelected.size(); i++) {
                final PurchaseOrder pur = defaultSelected.get(i);
                purOrderList.remove(pur);purOrderList.add(i,pur);
                final LinearLayout row = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.pur_item_choice, null);
                ((TextView) row.findViewById(R.id.pur_supplier_item)).setText(pur.getSupplier());
                ((TextView) row.findViewById(R.id.pur_date_item)).setText(pur.getDate());
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        fullScreenShowPurOrder(pur);
                    }
                });
                CheckBox checkBox = row.findViewById(R.id.pur_item_checkBox);
                checkBox.setChecked(true);
                purChoices.add(pur);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        if (isChecked) {
                            purChoices.add(pur);
                        } else {
                            purChoices.remove(pur);
                        }
                    }
                });
                tableBody.addView(row);
            }
            purOrderList.add(0,null);
        }
        if (isEmpty){
            LinearLayout v = new LinearLayout(context);
            v.setMinimumHeight(15);
            WindowManager.LayoutParams attr = dialog.getWindow().getAttributes();
            v.setMinimumWidth((int)(attr.width*0.91f));
            v.setBackgroundResource(R.drawable.edit_background);
            tableBody.addView(v);
            TextView t2 = new TextView(context);
            t2.setText("\n         没有查询到相关进货单");
            tableBody.addView(t2);
        }
        else if (!sizeEqual){
            TextView tv = new TextView(context);
            tv.setText("新增请勾选：");
            tv.setTextColor(Color.rgb(216,27,96));
            tableBody.addView(tv);
            purOrderList.add(defaultSelected.isEmpty()?0:defaultSelected.size()+1,null);
            for(final PurchaseOrder pur:purOrderList){
                if (pur==null)continue;
                if (defaultSelected.contains(pur))continue;
                final LinearLayout row=(LinearLayout) LayoutInflater.from(context).inflate(R.layout.pur_item_choice,null);
                ((TextView)row.findViewById(R.id.pur_supplier_item)).setText(pur.getSupplier());
                ((TextView)row.findViewById(R.id.pur_date_item)).setText(pur.getDate());
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        fullScreenShowPurOrder(pur);
                    }
                });
                CheckBox checkBox = row.findViewById(R.id.pur_item_checkBox);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        if (isChecked) {
                            purChoices.add(pur);
                        } else {
                            purChoices.remove(pur);
                        }
                    }
                });
                tableBody.addView(row);
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
                if (!purChoices.equals(defaultSelected)){
                    CrudService service = new CrudService(context);
                    String msg="操作失败！";
                    if (service.bindingPurOrder2Goods(purChoices,goods.getId())) {
                        msg="操作成功！";purListView.removeAllViews();
                        if (purChoices.size()<1){
                            TextView tV = new TextView(context);
                            tV.setWidth(tableHeader.getWidth());
                            tV.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            tV.setText("该商品未添加进货单！");
                            tV.setTextSize(24);
                            purListView.addView(tV);
                        }else{// TODO: 2020/10/30 冗余 待优化
                        for (int i = 0; i < purChoices.size(); i++) {
                            final PurchaseOrder pur = purChoices.get(i);
                            final LinearLayout row = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.list_item, null);
                            ((TextView) row.findViewById(R.id.pur_supplier_item)).setText(pur.getSupplier());
                            ((TextView) row.findViewById(R.id.pur_date_item)).setText(pur.getDate());
                            setPurItemOnClickEvent(context,row,pur,i,StatusCode.IN_PURLIST_VIEW);
                            purListView.addView(row);
                        }
                        asynchronousLoadPurImgs(context,purListView,purChoices);}
                    }
                    service.close();
                    ToastUtil.showShortToast(msg);
                }
                dialog.dismiss();
            }
        });
        header.addView(button);
        asynchronousLoadPurImgs(context,tableBody,purOrderList);
    }

    //调用相机 拍照 进货单
    private void useCameraForAddPurOrder(Context context){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//设置动作为调用照相机
        File file = new MainActivity().createPhotoFile();
        if (file!=null){
            MainActivity.showPIC=file;
            Uri imgUri= FileProvider.getUriForFile(context,context.getPackageName()+".fileprovider", file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT,imgUri);//指定系统相机拍照保存在imageFileUri所指的位置
        }
        startActivityForResult(intent, StatusCode.REQUEST_CODE_SHOOT);
    }
}