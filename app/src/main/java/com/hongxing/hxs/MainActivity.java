package com.hongxing.hxs;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
import com.hongxing.hxs.service.CrudService;
import com.hongxing.hxs.ui.dialog.ScanResultDialog;
import com.hongxing.hxs.utils.GoodsUtils;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    public static final int DEFAULT_VIEW = 0x22;

    private static final int REQUEST_CODE_SCAN = 0X01;

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
    }

    public void setThisGoodsByBarcode(String barcode) throws UnsupportedEncodingException {
        CrudService crudService=new CrudService(this);
        goods=crudService.findByBarcode(barcode);
        if(goods==null){
            Toast.makeText(this, "没有录入与"+barcode+"对应的商品！", Toast.LENGTH_SHORT).show();
            goods=new Goods();
            goods.setBarcode(barcode);
            showAddGoodsPage();
        }else{
            Intent intent = new Intent(MainActivity.this, ScanResultDialog.class);
            startActivity(intent);
        }
    }

    public void addGoodsClick(View v){
        if (goods==null)goods=new Goods();
        showAddGoodsPage();
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
                boolean ok = GoodsUtils.checkGoodsInfo(MainActivity.this,goods,map,GoodsUtils.DO_ADD);
                if (ok) dialog.dismiss();
            }
        });
    }

//    public boolean checkAddGoodsInfo(HashMap<String,String> map) {
//        ArrayList<String> list = new ArrayList<>();
//        CrudService service = new CrudService(this);
//        if ("".equals(map.get("name"))){
//            list.add(" 商品名称不能为空！");
//        }else goods.setName(map.get("name"));
//        if ("".equals(map.get("barcode"))){
//            goods.setBarcode("无");
//        }else{
//            if (map.get("barcode").length()!=13)
//                list.add(" 请输入正确的13位商品条码！");
//            goods.setBarcode(map.get("barcode"));
//        }
//        if ("请选择".equals(map.get("unit"))){
//            list.add(" 请选择商品单位！");
//        }else goods.setUnit(map.get("unit"));
//        if ("".equals(map.get("price"))){
//            list.add(" 商品售价不能为空！");
//        }else goods.setPrice(Float.valueOf(Objects.requireNonNull(map.get("price"))));
//        if ("".equals(map.get("orig"))){
//            map.put("orig","0.00");
//        }goods.setOrig(Float.valueOf(Objects.requireNonNull(map.get("orig"))));
//
//        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
//        dialog.setTitle("提示");
//        dialog.setIcon(R.drawable.error);
//        if(!list.isEmpty()){
//            dialog.setMessage(list.get(0));
//            dialog.show();
//            service.close();
//            return false;
//        }else{
//            String name = map.get("name");
//            String unit = map.get("unit");
//            boolean exist = service.existGoodsByNameAndUnit(name,map.get("unit"));
//            if (exist){
//                dialog.setMessage("已存在名称为 “"+name+"” 且单位为 “"+unit+"” 的商品,请勿重复添加！");
//                dialog.show();
//                service.close();
//                return false;
//            }
//        }
//        service.save(goods);
//        Toast.makeText(MainActivity.this, "添加成功！", Toast.LENGTH_LONG).show();
//        service.close();
//        return true;
//    }
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
