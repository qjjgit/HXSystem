package com.hongxing.hxs;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hongxing.hxs.db.DBManager;
import com.hongxing.hxs.entity.Goods;
import com.hongxing.hxs.entity.PurchaseOrder;
import com.hongxing.hxs.other.MyViewBinder;
import com.hongxing.hxs.service.CrudService;
import com.hongxing.hxs.utils.DateUtils;
import com.hongxing.hxs.utils.GoodsUtils;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final int DEFAULT_VIEW = 0x22;
    private static final int REQUEST_CODE_SCAN = 0X01;
    private static final int REQUEST_CODE_SHOOT = 0X02;

    private static boolean isQuit = false;
    private Timer timer = new Timer();
    public static Goods goods;
    private View nowTopLayerView;
    private ListView listView_PurOrderBody=null;
    private List<Map<String,Object>> lists=null;
    private SimpleAdapter adapter=null;
    private File showPIC=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
        DBManager.openDatabase(this).close();
    }
    /*开始扫码*/
    public void btnScanClick(View v){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.requestPermissions(
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                    DEFAULT_VIEW);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (permissions == null || grantResults == null || grantResults.length < 2 || grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (requestCode == DEFAULT_VIEW) {
            //start ScankitActivity for scanning barcode
            ScanUtil.startScan(MainActivity.this, REQUEST_CODE_SCAN, new HmsScanAnalyzerOptions.Creator().setHmsScanTypes(HmsScan.ALL_SCAN_TYPE).create());
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //receive result after your activity finished scanning
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK ) {
            if (requestCode==REQUEST_CODE_SHOOT){
                showPIC.delete();
            }
            return;
        }
        // Obtain the return value of HmsScan from the value returned by the onActivityResult method by using ScanUtil.RESULT as the key value.
        if (requestCode == REQUEST_CODE_SCAN) {
            Object obj = data.getParcelableExtra(ScanUtil.RESULT);
            if (obj instanceof HmsScan) {
                String barcode = ((HmsScan) obj).getOriginalValue();
                if (!TextUtils.isEmpty(barcode)) {
                    if(barcode.length()==13){
//                        Toast.makeText(this, resultText, Toast.LENGTH_SHORT).show();
                        setThisGoodsByBarcode(barcode);
                    }else{
                        Toast.makeText(this, "请扫描商品条码！", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        //拍照 进货单结果
        if (requestCode == REQUEST_CODE_SHOOT){
            View view= LayoutInflater.from(this).inflate(R.layout.suretoadd_purorder_page, null);
            final Bitmap bitmap_orig = BitmapFactory.decodeFile(showPIC.getPath());
            final Bitmap bitmap_comp=centerSquareScaleBitmap(bitmap_orig, 150,getResources().getDisplayMetrics().density);
            ImageView imgView=view.findViewById(R.id.img_addingPurOrder);
            imgView.setImageBitmap(bitmap_comp);
            AlertDialog.Builder builder= new AlertDialog.Builder(this);
            final Dialog dialog= builder.create();
            dialog.show();
            dialog.getWindow().setContentView(view);
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            dialog.setCancelable(false);
            (view.findViewById(R.id.addPurOrder_cancel)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    showPIC.delete();
                }
            });
            final EditText addPurOrder_date= view.findViewById(R.id.addPurOrder_date);
            final EditText addPurOrder_supplier= view.findViewById(R.id.addPurOrder_supplier);
            (view.findViewById(R.id.addPurOrder_ok)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String strDate = (addPurOrder_date.getText().toString()).replace(" ","");
                    if ("".equals(strDate)){
                        Toast.makeText(getApplicationContext(),"请先填入进货单的年月日时间！",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!DateUtils.isValidDate(strDate)){
                        Toast.makeText(getApplicationContext(),"请填入正确的年月日！",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String supplier =addPurOrder_supplier.getText().toString().replaceAll(" ","");
                    if (supplier.length()<1)supplier="未填写";
                    sureToAdd_purOrder(bitmap_comp,strDate,supplier);
                    dialog.dismiss();
                }
            });
        }

    }
    //确认添加进货单
    public void sureToAdd_purOrder(Bitmap bitmap_comp,String strDate,String supplier){
        //将图片转化为位图
//        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
//        int byteCount = bitmap_orig.getByteCount();
        //int size = 20 * 30 * 4;
        //创建一个字节数组输出流,流的大小为size
//        ByteArrayOutputStream baos= new ByteArrayOutputStream(byteCount);
        try {
            //设置位图的压缩格式，质量为100%，并放入字节数组输出流中
//            bitmap_orig.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            //将字节数组输出流转化为字节数组byte[]
//            byte[] imagedata = baos.toByteArray();
            PurchaseOrder purchaseOrder = new PurchaseOrder(UUID.randomUUID().toString(),supplier,strDate,showPIC.getPath());
            CrudService service = new CrudService(getApplicationContext());
            service.savePurchaseOrder(goods.getId(),purchaseOrder);
            service.close();
            Map<String, Object> map = new HashMap<>();
            map.put("origImgUri",showPIC.getPath());
            map.put("compImg",bitmap_comp);
            map.put("supplier",purchaseOrder.getSupplier());
            map.put("date",purchaseOrder.getDate());
            if (lists==null){
                lists=new ArrayList<>();
            }
            lists.add(map);
            if (listView_PurOrderBody!=null){
                ((TextView)nowTopLayerView.findViewById(R.id.notFoundWord)).setTextSize(0);
                adapter = new SimpleAdapter(this, lists, R.layout.list_item,
                        new String[]{"compImg","supplier","date"}, new int[]{R.id.pur_img_item,R.id.pur_supplier_item,R.id.pur_date_item});
                adapter.setViewBinder(new MyViewBinder());
                listView_PurOrderBody.setAdapter(adapter);
            }
            Toast.makeText(getApplicationContext(),"进货单添加成功！",Toast.LENGTH_SHORT).show();
        }catch (Exception e){
//            e.printStackTrace();
            Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    //通过barcode设置当前goods属性
    public void setThisGoodsByBarcode(String barcode) {
        CrudService service=new CrudService(this);
        goods=service.findByBarcode(barcode);
        service.close();
        if(goods==null){
            Toast.makeText(this, "没有录入与"+barcode+"对应的商品！", Toast.LENGTH_SHORT).show();
            goods=new Goods();
            goods.setBarcode(barcode);
            showAddGoodsPage();
        }else{
//            Intent intent = new Intent(MainActivity.this, ScanResultDialog.class);
//            startActivity(intent);
            showScanResultPage();
        }
    }

    //点击 添加商品  by click
    public void addGoodsClick(View v){
        goods=new Goods();
        showAddGoodsPage();
    }

    //扫码结果页面
    public void showScanResultPage(){
        View view= LayoutInflater.from(this).inflate(R.layout.dialog_scan_result, null);
        ((TextView)view.findViewById(R.id.goodsName)).setText(goods.getName());
        ((TextView)view.findViewById(R.id.barcode)).setText(goods.getBarcode());
        ((TextView)view.findViewById(R.id.unit)).setText(goods.getUnit());
        ((TextView)view.findViewById(R.id.price)).setText((goods.getPrice() +"元"));
        final TextView orig=view.findViewById(R.id.orig);
        orig.setText("****元");
        orig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                orig.setText((goods.getOrig()+"元"));
            }
        });
        AlertDialog.Builder builder= new AlertDialog.Builder(MainActivity.this);
        final Dialog dialog= builder.create();
        dialog.show();
        dialog.getWindow().setContentView(view);
    }

    //添加商品页面
    public void showAddGoodsPage(){
        View view= LayoutInflater.from(this).inflate(R.layout.dialog_add_goods_page, null);
        final TextView cancel =view.findViewById(R.id.addGoods_cancel);
        final TextView sure =view.findViewById(R.id.addGoods_sure);
        final EditText eText_name =view.findViewById(R.id.addGoods_name);
        final Spinner spinner_unit =view.findViewById(R.id.addGoods_unitList);
        final TextView tView_barcode=view.findViewById(R.id.addGoods_barcode);
        final EditText eText_price =view.findViewById(R.id.addGoods_price);
        final EditText eText_orig =view.findViewById(R.id.addGoods_orig);
        AlertDialog.Builder builder= new AlertDialog.Builder(MainActivity.this);
        final Dialog dialog= builder.create();
        dialog.show();
        dialog.getWindow().setContentView(view);
        dialog.setCancelable(false);
        if (goods.getBarcode()!=null){
            tView_barcode.setText(goods.getBarcode());
            tView_barcode.setFocusable(false);
        }
        //使editext可以唤起软键盘
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        spinner_unit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String item = spinner_unit.getSelectedItem().toString();
                goods.setUnit(item);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
//                System.out.println("没有选择单位，已设为默认值“个”");
                goods.setUnit("个");
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(MainActivity.this, "您已取消添加", Toast.LENGTH_SHORT).show();
                goods=null;
                dialog.dismiss();
            }
        });
        sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashMap<String, String> map = new HashMap<>();
                map.put("name",eText_name.getText().toString());
                map.put("barcode",tView_barcode.getText().toString());
                map.put("unit",goods.getUnit());
                map.put("price",eText_price.getText().toString());
                map.put("orig",eText_orig.getText().toString());
                boolean ok = GoodsUtils.checkGoodsInfoForAction(MainActivity.this,goods,map,GoodsUtils.DO_ADD);
                if (ok) dialog.dismiss();
            }
        });
    }

    //进货单列表页面  by click
    public void showPurchaseOrderPage(View view){
        final Context context = this;
        View root= LayoutInflater.from(context).inflate(R.layout.purchaseorder_page, null);
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);// TODO: 2020/10/6 看看有没有用
        nowTopLayerView=root;
        AlertDialog.Builder builder= new AlertDialog.Builder(context,R.style.Dialog_Fullscreen);
        final Dialog dialog= builder.create();
        dialog.show();
        dialog.getWindow().setContentView(root);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode== KeyEvent.KEYCODE_BACK&&event.getAction()==KeyEvent.ACTION_UP){
                    lists.clear();
                    dialog.dismiss();
                }
                return true;
            }
        });
        if (lists==null)
            lists=new ArrayList<>();
        else lists.clear();
        listView_PurOrderBody=root.findViewById(R.id.list_purOrder);
        listView_PurOrderBody.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Context context = view.getContext();
                View root= LayoutInflater.from(context).inflate(R.layout.show_bitmap_full, null);
                AlertDialog.Builder builder= new AlertDialog.Builder(context,R.style.Dialog_Fullscreen);
                final Dialog dialog= builder.create();
                dialog.show();
                dialog.getWindow().setContentView(root);
                ImageView imgV=root.findViewById(R.id.img_fullscreen);
                if (lists.get(i).get("origImgUri") instanceof String){
                    String uri=(String) lists.get(i).get("origImgUri");
                    Bitmap bitmap = BitmapFactory.decodeFile(uri);
                    imgV.setImageBitmap(bitmap);
                    imgV.setScaleType(ImageView.ScaleType.FIT_CENTER);
                }else imgV.setImageResource(R.mipmap.ic_launcher);
//                    imgV.setOnClickListener(new View.OnClickListener(){
//                        @Override
//                        public void onClick(View v){
//                            dialog.dismiss();
//                        }
//                    });
            }
        });
        CrudService service = new CrudService(context);
        try {
            ArrayList<PurchaseOrder> purOrderList = service.getPurOrderListByGoodsId(goods.getId());
            service.close();
            TextView textV_notFound=root.findViewById(R.id.notFoundWord);
            if (purOrderList.size()<1){
                textV_notFound.setTextSize(24);
                return;
            }
            for (PurchaseOrder po : purOrderList) {
                Map<String,Object> map=new HashMap<>();
                String imgUri = po.getDataUri();
                map.put("origImgUri",imgUri);
                Bitmap bitmap = BitmapFactory.decodeFile(imgUri);
                bitmap=centerSquareScaleBitmap(bitmap,100,getResources().getDisplayMetrics().density);
                map.put("compImg",bitmap);
                map.put("supplier",po.getSupplier());
                map.put("date",po.getDate());
                lists.add(map);
            }
            adapter = new SimpleAdapter(this, lists, R.layout.list_item,
                    new String[]{"compImg","supplier","date"}, new int[]{R.id.pur_img_item,R.id.pur_supplier_item,R.id.pur_date_item});
            adapter.setViewBinder(new MyViewBinder());
            listView_PurOrderBody.setAdapter(adapter);
        } catch (Exception e) {
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
//            e.printStackTrace();
        }
    }

    //添加进货单  by click
    public void addPurOrderPage(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//设置动作为调用照相机
        File file = createPhotoFile();
        if (file!=null){
            showPIC=file;
            Uri imgUri= FileProvider.getUriForFile(this, "comgin.example.root.hightqualitycame.fileprovider", file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT,imgUri);//指定系统相机拍照保存在imageFileUri所指的位置
        }
        startActivityForResult(intent, REQUEST_CODE_SHOOT);
    }

    //创建进货单图像文件
    public File createPhotoFile() {
//        String strdir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath()
//                +File.separator+"HongXing";
        /*若要修改路径，需同时修改file_paths.xml内声明的根目录*/
        String strdir=this.getExternalFilesDir(null).getPath()
                +File.separator+"PurchaseOrder";
        File stordir = new File(strdir);
        if (!stordir.exists()) stordir.mkdir();
        //获得公共目录下的图片文件路径
        File image= null;
        try {
            image = File.createTempFile("IMG_",".jpg",stordir);
        } catch (IOException e) {
            Toast.makeText(this,"createPhotoFile()异常\n"+e.getMessage(),Toast.LENGTH_LONG).show();
        }
        //1：字首2：后缀3：在哪个目录下
        return  image;
    }

    /**
     * 缩放图片裁剪指定大小的正方形
     * @param bitmap 原图
     * @param edgeLength 正方形的边长
     * @return 裁剪后的图片
     */
    public static Bitmap centerSquareScaleBitmap(Bitmap bitmap, int edgeLength, float scale){
        if(null == bitmap || edgeLength <= 0){
            return null;
        }
        Bitmap result = bitmap;
        edgeLength=(int)(edgeLength * scale + 0.5f);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if(width > edgeLength && height > edgeLength){
            int longerEdge = (int)(edgeLength * Math.max(width, height) / Math.min(width, height));
            int scaleWidth = width > height ? longerEdge : edgeLength;
            int scaleHeight = width > height ? edgeLength : longerEdge;
            Bitmap scaleBitmap;
            try{
                scaleBitmap = Bitmap.createScaledBitmap(bitmap, scaleWidth, scaleHeight, true);
            }catch(Exception e){
                return null;
            }
            //从图片的正中间裁剪
            int xTopLeft = (scaleWidth - edgeLength)/2;
            int yTopLeft = (scaleHeight - edgeLength)/2;
            try{
                result = Bitmap.createBitmap(scaleBitmap, xTopLeft, yTopLeft, edgeLength, edgeLength);
                scaleBitmap.recycle();
            }catch(Exception e){
                return null;
            }
        }
        return result;
    }

    //将dp转换成px
//    public int dip2px(int dpSize){
//        final float scale = getResources().getDisplayMetrics().density;
//        return (int)(dpSize * scale + 0.5f);
//    }

    //导出数据
    public void exportDB(View view){
        String strdir=Environment.getExternalStorageDirectory().getPath()
                +File.separator+"鸿兴系统";
        File file = new File(strdir);
        if (!file.exists())file.mkdir();
        boolean success = DBManager.exportDBFileToDir(strdir);
        String msg="备份失败！";
        if (success) msg="已备份到"+strdir;
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show();
    }

    /* 回退按钮两次退出 */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!isQuit) {
                isQuit = true;
                Toast.makeText(this, "再按一次返回键退出", Toast.LENGTH_SHORT).show();
                TimerTask task = null;
                task = new TimerTask() {
                    @Override
                    public void run() {
                        isQuit = false;
                    }
                };
                timer.schedule(task, 2000);
            } else {
                MainActivity.this.finish();
                System.exit(0);
            }
        }
        return true;
    }
}
