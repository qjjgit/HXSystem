package com.hongxing.hxs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hongxing.hxs.entity.Goods;
import com.hongxing.hxs.entity.PurchaseOrder;
import com.hongxing.hxs.service.CrudService;
import com.hongxing.hxs.utils.GoodsUtils;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public static final int DEFAULT_VIEW = 0x22;
    private static final int REQUEST_CODE_SCAN = 0X01;
    private static final int REQUEST_CODE_SHOOT = 0X02;

    private static boolean isQuit = false;
    private Timer timer = new Timer();
    public static Goods goods;

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
        if (resultCode != RESULT_OK || data == null) {
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
                        try {
                            setThisGoodsByBarcode(barcode);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }else{
                        Toast.makeText(this, "请扫描商品条码！", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        if (requestCode == REQUEST_CODE_SHOOT){
            Bundle bundle = data.getExtras();
            Bitmap bitmap = (Bitmap) bundle.get("data"); //将data中的信息流解析为Bitmap类型
            //iv_pic.setImageBitmap(bitmap);// 显示图片
            //将图片转化为位图
            int size = bitmap.getWidth() * bitmap.getHeight() * 4;
            //int size = 20 * 30 * 4;
            //创建一个字节数组输出流,流的大小为size

            ByteArrayOutputStream baos= new ByteArrayOutputStream(size);
            try {
                //设置位图的压缩格式，质量为100%，并放入字节数组输出流中
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                //将字节数组输出流转化为字节数组byte[]
                byte[] imagedata = baos.toByteArray();
                PurchaseOrder purchaseOrder = new PurchaseOrder();
                purchaseOrder.setData(imagedata);
//                dbop.insert(d);
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try {
                    //bitmap.recycle();
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setThisGoodsByBarcode(String barcode) throws UnsupportedEncodingException {
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

    public void addGoodsClick(View v){
        if (goods==null)goods=new Goods();
        showAddGoodsPage();
    }

    public void showScanResultPage(){
        View view= LayoutInflater.from(this).inflate(R.layout.dialog_scan_result, null);
        ((TextView)view.findViewById(R.id.goodsName)).setText(goods.getName());
        ((TextView)view.findViewById(R.id.barcode)).setText(goods.getBarcode());
        ((TextView)view.findViewById(R.id.unit)).setText(goods.getUnit());
        ((TextView)view.findViewById(R.id.price)).setText((goods.getPrice() +"元"));
        ((TextView)view.findViewById(R.id.orig)).setText("****元");
        AlertDialog.Builder builder= new AlertDialog.Builder(MainActivity.this);
        final Dialog dialog= builder.create();
        dialog.show();
        dialog.getWindow().setContentView(view);
    }
    public void showOrig(View view){
        ((TextView)findViewById(R.id.orig)).setText((goods.getOrig().toString()+"元"));
    }

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

    public void showPurchaseOrder(View v){
        View view= LayoutInflater.from(this).inflate(R.layout.purchaseorder_page, null);
        view.setMinimumWidth(360);
        view.setMinimumHeight(480);
        AlertDialog.Builder builder= new AlertDialog.Builder(MainActivity.this);
        final Dialog dialog= builder.create();
        dialog.show();
        dialog.getWindow().setContentView(view);
    }

    public void addPurOrder(View view){
        Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE); //设置动作为调用照相机
        startActivityForResult(intent, REQUEST_CODE_SHOOT);
    }

    /*
     * 回退按钮两次退出
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!isQuit) {
                isQuit = true;
                Toast.makeText(this, "请按两次回退键退出", Toast.LENGTH_SHORT).show();
                TimerTask task = null;
                task = new TimerTask() {
                    @Override
                    public void run() {
                        isQuit = false;
                    }
                };
                timer.schedule(task, 2000);
            } else {
                finish();
                System.exit(0);
            }
        }
        return true;
    }
}
