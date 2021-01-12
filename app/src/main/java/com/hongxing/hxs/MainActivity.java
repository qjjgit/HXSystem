package com.hongxing.hxs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hongxing.hxs.db.DBManager;
import com.hongxing.hxs.entity.Goods;
import com.hongxing.hxs.service.CrudService;
import com.hongxing.hxs.ui.dashboard.DashboardFragment;
import com.hongxing.hxs.ui.dialog.MyDialog;
import com.hongxing.hxs.utils.CommonUtils;
import com.hongxing.hxs.utils.ToastUtil;
import com.hongxing.hxs.utils.http.HttpUtils;
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
import java.util.Map;
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
    public static boolean isAdmin;
    Handler handler;

    @SuppressLint({"WrongConstant", "HandlerLeak"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertDialog.Builder builder= new AlertDialog.Builder(this,R.style.Dialog_Fullscreen);
        final Dialog dialog= builder.create();dialog.setCancelable(false);
        dialog.show();dialog.getWindow().setContentView(R.layout.initialization_page);
        handler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) { super.handleMessage(msg);
                if (msg.what==0x00) dialog.cancel();
            }
        };
//        setContentView(R.layout.activity_main);
////        BottomNavigationView navView = findViewById(R.id.nav_view);
////        // Passing each menu ID as a set of Ids because each
////        // menu should be considered as top level destinations.
////        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
////                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
////                .build();
////        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
////        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
////        NavigationUI.setupWithNavController(navView, navController);
        appContext=getApplicationContext();
        APPStoragePath= CommonUtils.getAPPStoragePath(appContext);
        checkPermissions();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.scan_btn, menu);//引用menu布局文件R.menu.scan_btn
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        MenuItem item = menu.findItem(R.menu.scan_btn);
//        if (item.isEnabled())item.setEnabled(false);
//        else item.setEnabled(true);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if (item.getItemId() == R.id.scan_btn) {//监听菜单按钮
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.requestPermissions(
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                        DEFAULT_VIEW);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void init(){
        CommonUtils.checkRequiredFolder();//检查必要文件夹
        DBManager.openDatabase(this).close();//检查是否已有DB文件
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
        handler.sendEmptyMessageDelayed(0x00,800);
        isAdmin= HttpUtils.isAdmin();//从服务器检查，是否是管理员
        if (!isAdmin)return;//是管理员 则定时检查DB文件 有改动则上传至服务器
        DBManager.db_lastModified= DBManager.db_file.lastModified();
//        Handler handler=new Handler();
        Runnable runnable=new Runnable(){
            @Override
            public void run() {
                long now = DBManager.db_file.lastModified();
                if (now - DBManager.db_lastModified > 1000){
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
                    DBManager.db_lastModified=now;
                }
                handler.postDelayed(this, 10000);
            }
        };
        handler.postDelayed(runnable, 10000);
    }
    private void checkPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.requestPermissions(
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.INTERNET},
                    0X03);
        }
    }

    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        Configuration config = new Configuration();
        config.setToDefaults();
        res.updateConfiguration(config,res.getDisplayMetrics());
        return res;
    }

    //获取系统上下文context
    public static Context getMainContext(){
        return appContext;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0x03) {
            if (grantResults.length < 4 || grantResults[0] != PackageManager.PERMISSION_GRANTED||
                grantResults[1] != PackageManager.PERMISSION_GRANTED||
                grantResults[2] != PackageManager.PERMISSION_GRANTED||
                grantResults[3] != PackageManager.PERMISSION_GRANTED) {
                ToastUtil.showLongToastCenter("为确保软件正常使用，请授权相关权限");
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        checkPermissions();
                    }
                };
                timer.schedule(task, 2000);
            }else init();
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

    //进货单列表页面  by click
    public void showPurchaseOrderPage(View view){
        if (goods==null){
            ToastUtil.showShortToast("发生了错误!");return;
        }
        new DashboardFragment(this).showPurchaseOrderPage(this);
    }

    //创建进货单图像文件temp
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

    /* 回退按钮两次退出 */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!isQuit) {
                isQuit = true;
                ToastUtil.showShortToast("再按一次返回键退出");
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        isQuit = false;
                    }
                };
                timer.schedule(task, 2000);
            } else {
                MainActivity.this.finish();
            }
        }
        return true;
    }
}
