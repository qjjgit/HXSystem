package com.hongxing.hxs.ui.notifications;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.hongxing.hxs.MainActivity;
import com.hongxing.hxs.R;
import com.hongxing.hxs.db.DBManager;
import com.hongxing.hxs.service.CrudService;
import com.hongxing.hxs.utils.CommonUtils;
import com.hongxing.hxs.utils.ToastUtil;
import com.hongxing.hxs.utils.http.HttpUtils;
import com.hongxing.hxs.utils.zip.CompressListener;
import com.hongxing.hxs.utils.zip.ZIPUtils;
import com.hongxing.hxs.utils.zip.UnzipListener;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static android.app.Activity.RESULT_OK;


public class NotificationsFragment extends Fragment {

    private NotificationsViewModel notificationsViewModel;
    private TextView lastBackup;
    private Handler handler;
    private ProgressDialog progressDialog;

    @SuppressLint("HandlerLeak")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        notificationsViewModel =
                ViewModelProviders.of(this).get(NotificationsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);
        final Context context = getContext();
        progressDialog = new ProgressDialog(context);
        handler=new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                //上传初始化
                if (msg.what==0x05){
                    progressDialog.setProgress(0);
                    progressDialog.setCancelable(false);
                    progressDialog.setTitle("数据上传中……");
                    progressDialog.setMax(100);
                    progressDialog.setIcon(R.drawable.data_ico32);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.show();
                }
                //下载初始化
                if (msg.what==0x12){
                    progressDialog.setProgress(0);
                    progressDialog.setCancelable(false);
                    progressDialog.setTitle("正在从数据中心获取数据……");
                    progressDialog.setMax(100);
                    progressDialog.setIcon(R.drawable.data_ico32);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.show();
                }
                //解压初始化
                if (msg.what==0x14){
                    progressDialog.setProgress(0);
                    progressDialog.setCancelable(false);
                    progressDialog.setTitle("数据导入中……");
                    progressDialog.setMax(100);
                    progressDialog.setIcon(R.drawable.data_ico32);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.show();
                }
                //更新进度
                if (msg.what==0x07){
                    Bundle data = msg.getData();
                    int progress = data.getInt("progress");
                    progressDialog.setProgress(progress);
                }
                //解压结束
                if (msg.what==0x06){
                    Toast.makeText(context, "数据导入完成!", Toast.LENGTH_LONG).show();
                    progressDialog.cancel();
                    progressDialog.setProgress(0);
                }
                //解压失败
                if (msg.what==0x08){
                    Toast.makeText(context, "数据导入失败!", Toast.LENGTH_LONG).show();
                    progressDialog.cancel();
                    progressDialog.setProgress(0);
                }
                //下载数据成功，开始解压
                if (msg.what==0x09){
                    Toast.makeText(context, "数据获取完成，开始导入!", Toast.LENGTH_SHORT).show();
                    progressDialog.cancel();
                    progressDialog.setProgress(0);
                }
                //下载失败
                if (msg.what==0x11){
                    Bundle data = msg.getData();
                    String response = data.getString("response");
                    Toast.makeText(context, response, Toast.LENGTH_LONG).show();
                    progressDialog.cancel();
                    progressDialog.setProgress(0);
                }
                //上传成功
                if (msg.what==0x13){
                    Bundle data = msg.getData();
                    String response = data.getString("response");
                    Toast.makeText(context, response, Toast.LENGTH_LONG).show();
                    progressDialog.cancel();
                    progressDialog.setProgress(0);
                }
                //上传失败
                if (msg.what==0x10){
                    Bundle data = msg.getData();
                    String response = data.getString("response");
                    Toast.makeText(context, response, Toast.LENGTH_LONG).show();
                    progressDialog.cancel();
                    progressDialog.setProgress(0);
                }
                //开始压缩(备份)
                if(msg.what==0x15){
                    progressDialog.setCancelable(false);
                    progressDialog.setTitle("正在备份……");
                    progressDialog.setIcon(R.drawable.data_ico32);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progressDialog.show();
                }
                //压缩(备份到磁盘)成功
                if(msg.what==0x16){
                    String date = (String) msg.obj;
                    lastBackup.setText(("上一次备份："+date));
                    Toast.makeText(context,"已备份到"+CommonUtils.getBackupPath(), Toast.LENGTH_LONG).show();
                    progressDialog.cancel();
                }
                //压缩(备份)失败成功
                if(msg.what==0x17){
                    Toast.makeText(context,"备份失败", Toast.LENGTH_LONG).show();
                    progressDialog.cancel();
                }
            }
        };
        setClick(root);
        lastBackup = root.findViewById(R.id.text_lastBackup);
        notificationsViewModel.getText().observe(this, s -> {
            CrudService service = new CrudService(getContext());
            String date = service.getLastBackup();service.close();
            lastBackup.append(date);
        });
        return root;
    }

    private void setClick(View view){
        final Context context = getContext();
        final String strdir = CommonUtils.getBackupPath();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd.HH.mm", Locale.CHINA);
        view.findViewById(R.id.btn_exportData).setOnClickListener(v -> {
            try {
                Date date = format.parse(lastBackup.getText().toString().replace("上一次备份：", ""));
                long l = new Date().getTime() - date.getTime();
                int minute = (int)(l / 60000);
                if (minute<3){
                    ToastUtil.showShortToast("您刚刚进行过备份，请勿频繁操作！");
                    return;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            File file = new File(strdir);
            if (!file.exists()) file.mkdir();
            String date = format.format(new Date());
            DBManager.exportDBFileToDir(strdir+File.separator+"鸿兴系统"+date,new CompressListener() {
                @Override
                public void zipStart() {
                    handler.sendEmptyMessage(0x15);
                }
                @Override
                public void zipSuccess() {
                    CrudService service = new CrudService(context);
                    service.setLastBackup(date);
                    service.close();
                    Message msg = new Message();
                    msg.obj=date;msg.what=0x16;
                    handler.sendMessage(msg);
                }
                @Override
                public void zipFail() {
                    handler.sendEmptyMessage(0x17);
                }
            });
        });
        view.findViewById(R.id.btn_importData).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            File file = new File(strdir);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri uri = FileProvider.getUriForFile(context, getActivity().getPackageName()+".fileprovider", file);
                intent.setDataAndType(uri, "application/zip");
            } else {
                intent.setDataAndType(Uri.fromFile(file), "application/zip");
            }
            startActivityForResult(intent,0x04);
        });
        view.findViewById(R.id.doGetBtn).setOnClickListener(m -> {
            HttpUtils.doGet(null, new HttpUtils.Listener() {
                @Override
                public void startFileTransfer() { }
                @Override
                public void success(String response) {
                    String msg="与数据中心连接正常!";
                    if (response.contains("不在线"))msg="数据中心维护中!";
                    Looper.prepare();
                    ToastUtil.showShortToast(msg);
                    Looper.loop();
                }
                @Override
                public void progress(int progress) { }
                @Override
                public void error(Exception e) {
                    e.printStackTrace();
                    Looper.prepare();
                    ToastUtil.showShortToast("数据中心维护中!");
                    Looper.loop();
                }
            });
        });
        view.findViewById(R.id.doUploadBtn).setOnClickListener(a->{
            Map<String, String> map = new HashMap<>();
            String id = CommonUtils.getDeviceID(context);
            System.out.println(id);
            map.put("deviceID",id);map.put("date","2020-10-23 21.30");
            HttpUtils.uploadFile(map, new HttpUtils.Listener() {
                @Override
                public void startFileTransfer() {
                    handler.sendEmptyMessage(0x05);
                }
                @Override
                public void success(String response) {
                    System.out.println("response：\n"+response);
                    Bundle bundle = new Bundle();
                    bundle.putString("response", response);
                    Message msg = new Message();
                    msg.what =0x13;
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                }
                @Override
                public void progress(int progress) {
                    Bundle bundle = new Bundle();
                    bundle.putInt("progress", progress);
                    Message msg = new Message();
                    msg.what =0x07;
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                }
                @Override
                public void error(Exception e) {
                    Bundle bundle = new Bundle();
                    bundle.putString("response", e.getMessage());
                    Message msg = new Message();
                    msg.what =0x10;
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                    e.printStackTrace();
                }
            });
        });
        view.findViewById(R.id.doDownloadBtn).setOnClickListener(a->{
            HttpUtils.downloadFile(new HttpUtils.DownloadListener() {
                @Override
                public void startFileTransfer() {
                    handler.sendEmptyMessage(0x12);
                }
                @Override
                public void progress(int progress) {
                    Bundle bundle = new Bundle();
                    bundle.putInt("progress", progress);
                    Message msg = new Message();
                    msg.what =0x07;
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                }
                @Override
                public void success(File file) {
                    handler.sendEmptyMessage(0x09);
                    unzipFileToAPPStoragePath(file.getPath());
                }
                @Override
                public void success(String response) {

                }
                @Override
                public void error(Exception e) {
                    e.printStackTrace();
                    Bundle bundle = new Bundle();
                    bundle.putString("response", e.getMessage());
                    Message msg = new Message();
                    msg.what =0x11;
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                }
            });
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK ) {
            return;
        }
        //从本地导入数据
        if (requestCode==0x04){
            String relativePath = data.getData().getPath();
            final Context context = getContext();
            final String fileName = relativePath.substring(relativePath.lastIndexOf("/")+1);
            final String realFilePath=CommonUtils.getBackupPath()+File.separator+fileName;
            if (!fileName.contains("鸿兴系统")||!fileName.contains("备份")
                    ||!".zip".equals(fileName.substring(fileName.length() - 4))){
                ToastUtil.showShortToast("请选择正确的数据包！\n(数据包在 \"鸿兴超市\" 文件夹内)");
                return;
            }
            final AlertDialog.Builder alterDialog = new AlertDialog.Builder(context);
            alterDialog.setCancelable(false);
            alterDialog.setIcon(R.drawable.data_ico32);//图标
            alterDialog.setTitle("选择的数据包");//文字
            alterDialog.setMessage(" "+fileName);//提示消息
            //积极的选择
            alterDialog.setPositiveButton("取消", (dialog, which) -> {});
            //消极的选择
            alterDialog.setNegativeButton("确定",(dialog, which) -> unzipFileToAPPStoragePath(realFilePath));
            alterDialog.show();
        }
    }

    private void unzipFileToAPPStoragePath(String filePath){
        ZIPUtils.unzipFile(filePath, MainActivity.APPStoragePath.replace("/files",""), new UnzipListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void zipStart() {
                handler.sendEmptyMessage(0x14);
            }
            @Override
            public void zipSuccess() {
                new File(filePath).delete();
                handler.sendEmptyMessage(0x06);
            }
            @Override
            public void zipProgress(int progress) {
                Bundle bundle = new Bundle();
                bundle.putInt("progress", progress);
                Message msg = new Message();
                msg.what =0x07;
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
            @Override
            public void zipFail() {
                handler.sendEmptyMessage(0x08);
            }
        });
    }
}