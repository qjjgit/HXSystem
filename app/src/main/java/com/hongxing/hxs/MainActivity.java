package com.hongxing.hxs;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hongxing.hxs.db.DBManager;
import com.hongxing.hxs.entity.Goods;
import com.hongxing.hxs.service.CrudService;
import com.hongxing.hxs.ui.dashboard.DashboardFragment;
import com.hongxing.hxs.ui.dialog.MyDialog;
import com.hongxing.hxs.utils.CommonUtils;
import com.hongxing.hxs.utils.GoodsUtils;
import com.hongxing.hxs.utils.ScreenUtil;
import com.hongxing.hxs.utils.ToastUtil;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final int DEFAULT_VIEW = 0x22;
    private static final int REQUEST_CODE_SCAN = 0X01;

    private static boolean isQuit = false;
    private Timer timer = new Timer();
    public static Goods goods;
    public static File showPIC=null;
    public static String APPStoragePath;
    private static Context appContext;

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
        appContext=getApplicationContext();
        APPStoragePath= CommonUtils.getAPPStoragePath(getApplicationContext());
        DBManager.openDatabase(this).close();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.requestPermissions(
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0X03);
        }
//        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE){
            System.out.println("当前屏幕为横屏");
        }else{
            System.out.println("当前屏幕为竖屏");
        }
    }

    public static Context getMainContext(){
        return appContext;
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //receive result after your activity finished scanning
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK ) {return;}
        // Obtain the return value of HmsScan from the value returned by the onActivityResult method by using ScanUtil.RESULT as the key value.
        if (requestCode == REQUEST_CODE_SCAN) {
            Object obj = data.getParcelableExtra(ScanUtil.RESULT);
            if (obj instanceof HmsScan) {
                String barcode = ((HmsScan) obj).getOriginalValue();
                if (!TextUtils.isEmpty(barcode)) {
                    if(barcode.length()==13){
                        setThisGoodsByBarcode(barcode);
                    }else{
                        ToastUtil.showShortToast("请扫描商品条码！");
                    }
                }
            }
        }
    }

    //通过barcode设置当前goods属性
    public void setThisGoodsByBarcode(String barcode) {
        CrudService service=new CrudService(this);
        goods=service.findByBarcode(barcode);
        service.close();
        if(goods==null){
            ToastUtil.showShortToast("没有录入与"+barcode+"对应的商品！");
            goods=new Goods();
            goods.setBarcode(barcode);
            MyDialog.showAddGoodsPage(this);
        }else{
            showScanResultPage();
        }
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
//    public void showAddGoodsPage(){
//        View view= LayoutInflater.from(this).inflate(R.layout.dialog_add_goods_page, null);
//        final TextView cancel =view.findViewById(R.id.addGoods_cancel);
//        final TextView sure =view.findViewById(R.id.addGoods_sure);
//        final EditText eText_name =view.findViewById(R.id.addGoods_name);
//        final Spinner spinner_unit =view.findViewById(R.id.addGoods_unitList);
//        final TextView tView_barcode=view.findViewById(R.id.addGoods_barcode);
//        final EditText eText_price =view.findViewById(R.id.addGoods_price);
//        final EditText eText_orig =view.findViewById(R.id.addGoods_orig);
//        AlertDialog.Builder builder= new AlertDialog.Builder(MainActivity.this);
//        final Dialog dialog= builder.create();
//        dialog.show();
//        dialog.getWindow().setContentView(view);
//        dialog.setCancelable(false);
//        if (goods.getBarcode()!=null){
//            tView_barcode.setText(goods.getBarcode());
//            tView_barcode.setFocusable(false);
//        }
//        //使editext可以唤起软键盘
//        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
//        spinner_unit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                String item = spinner_unit.getSelectedItem().toString();
//                goods.setUnit(item);
//            }
//            @Override
//            public void onNothingSelected(AdapterView<?> adapterView) {
////                System.out.println("没有选择单位，已设为默认值“个”");
//                goods.setUnit("个");
//            }
//        });
//        cancel.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                goods=null;
//                dialog.dismiss();
//            }
//        });
//        sure.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                HashMap<String, String> map = new HashMap<>();
//                map.put("name",eText_name.getText().toString());
//                map.put("barcode",tView_barcode.getText().toString());
//                map.put("unit",goods.getUnit());
//                map.put("price",eText_price.getText().toString());
//                map.put("orig",eText_orig.getText().toString());
//                boolean ok = GoodsUtils.checkGoodsInfoForAction(MainActivity.this,goods,map,GoodsUtils.DO_ADD);
//                if (ok) dialog.dismiss();
//            }
//        });
//    }

    //进货单列表页面  by click
    public void showPurchaseOrderPage(View view){
        if (goods==null){
            ToastUtil.showShortToast("发生了错误!");return;
        }
        new DashboardFragment(this).showPurchaseOrderPage(this);
    }

    //创建进货单图像文件
    public File createPhotoFile() {
        /*若要修改路径，需同时修改file_paths.xml内声明的根目录*/
        String strdir=APPStoragePath+File.separator+"PurchaseOrder";
        File stordir = new File(strdir);
        if (!stordir.exists()) stordir.mkdir();
        //获得公共目录下的图片文件路径
        File image= null;
        try {
            image = File.createTempFile("IMG_",".jpg",stordir);
        } catch (IOException e) {
            ToastUtil.showShortToast("createPhotoFile异常\n"+e.getMessage());
        }
        return  image;
    }

    //将dp转换成px
//    public int dip2px(int dpSize){
//        final float scale = getResources().getDisplayMetrics().density;
//        return (int)(dpSize * scale + 0.5f);
//    }

    /* 回退按钮两次退出 */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!isQuit) {
                isQuit = true;
                ToastUtil.showShortToast("再按一次返回键退出");
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
