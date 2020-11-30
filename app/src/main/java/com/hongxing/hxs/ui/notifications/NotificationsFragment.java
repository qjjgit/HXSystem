package com.hongxing.hxs.ui.notifications;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

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
import com.hongxing.hxs.utils.zip.ZIPUtils;
import com.hongxing.hxs.utils.zip.ZipListener;

import org.apache.http.client.methods.HttpGet;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static android.app.Activity.RESULT_OK;


public class NotificationsFragment extends Fragment {

    private NotificationsViewModel notificationsViewModel;
    private TextView lastBackup;
    private Handler handler;

    @SuppressLint("HandlerLeak")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        notificationsViewModel =
                ViewModelProviders.of(this).get(NotificationsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);
        final ProgressDialog progressDialog = new ProgressDialog(getContext());
        handler=new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                //初始化
                if (msg.what==0x05){
                    progressDialog.setCancelable(false);
                    progressDialog.setMax(100);
                    progressDialog.setIcon(R.drawable.data_ico32);
                    progressDialog.setTitle("导入中……");
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.show();
                }
                //更新进度
                if (msg.what==0x07){
                    Bundle data = msg.getData();
                    int progress = data.getInt("progress");
                    progressDialog.setProgress(progress);
                }
                //结束
                if (msg.what==0x06){
                    ToastUtil.showShortToast("导入完成!");
                    progressDialog.cancel();
                }
                //失败
                if (msg.what==0x08){
                    ToastUtil.showShortToast("导入失败!");
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
        final String strdir = Environment.getExternalStorageDirectory().getPath()
                + File.separator + "鸿兴系统";
        view.findViewById(R.id.btn_exportData).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd.HH.mm", Locale.CHINA);
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
                boolean success = DBManager.exportDBFileToDir(strdir+File.separator+"鸿兴系统"+date);
                String msg="备份失败！";
                if (success){
                    CrudService service = new CrudService(context);
                    service.setLastBackup(date);
                    service.close();
                    lastBackup.setText(("上一次备份："+date));
                    msg="已备份到" + strdir;
                }
                ToastUtil.showShortToast(msg);
            }
        });
        view.findViewById(R.id.btn_importData).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });
        HttpUtils.Listener listener = new HttpUtils.Listener() {
            @Override
            public void success(String response) {
                System.out.println("response：\n"+response);
                Looper.prepare();
                ToastUtil.showShortToast(response);
                Looper.loop();
            }
            @Override
            public void error(String error_msg) {
                System.err.println("错误信息："+error_msg);
                Looper.prepare();
                ToastUtil.showShortToast(error_msg);
                Looper.loop();
            }
        };
        view.findViewById(R.id.doPostBtn).setOnClickListener(a->{
            Map<String,String> form=new HashMap<String,String>()
            {{put("phone","18593272979");put("password","123");put("method","post");}};
            String url = ((EditText) view.findViewById(R.id.et_url)).getText().toString();
            HttpUtils.doPost(url,form,listener);
        });
        view.findViewById(R.id.doGetBtn).setOnClickListener(a->{
            Map<String,String> parameters=new HashMap<String,String>()
                {{put("phone","18593272979");put("password","123");put("method","get");}};
            String url = ((EditText) view.findViewById(R.id.et_url)).getText().toString();
            HttpUtils.doGet(url,parameters,listener);
        });
        view.findViewById(R.id.doUploadBtn).setOnClickListener(a->{
            String path=strdir+File.separator+"鸿兴系统2020-11-25.16.24备份.zip";
            String url = ((EditText) view.findViewById(R.id.et_url)).getText().toString();
            Map<String, String> map = new HashMap<>();
            String id = CommonUtils.getDeviceID(context);
            map.put("deviceID",id);map.put("date","2020-10-23 21.30");
            HttpUtils.uploadFile(url, path,map,listener);
        });
        view.findViewById(R.id.doDownloadBtn).setOnClickListener(a->{
            HttpUtils.downloadFile(new HttpUtils.DownloadListener() {
                @Override
                public void startDownload() {

                }
                @Override
                public void progress(int progress) {

                }
                @Override
                public void success(File file) {
                    unzipFileToAPPStoragePath(file.getAbsolutePath());
                    file.delete();
                }
                @Override
                public void success(String response) {

                }
                @Override
                public void error(String error_msg) {
                    System.err.println("错误信息："+error_msg);
                    Looper.prepare();
                    ToastUtil.showShortToast(error_msg);
                    Looper.loop();
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
            final String realFilePath=Environment.getExternalStorageDirectory().getPath()
                    +File.separator +"鸿兴系统"+File.separator+fileName;
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
            alterDialog.setPositiveButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            });
            //消极的选择
            alterDialog.setNegativeButton("确定", new DialogInterface.OnClickListener() {
                @SuppressLint("HandlerLeak")
                @Override
                public void onClick(DialogInterface dialog, int which) {
//                    handler=new Handler(){
//                        @Override
//                        public void handleMessage(@NonNull Message msg) {
//                            super.handleMessage(msg);
//                            //初始化
//                            if (msg.what==0x05){
//                                progressDialog.setCancelable(false);
//                                progressDialog.setMax(100);
//                                progressDialog.setIcon(R.drawable.data_ico32);
//                                progressDialog.setTitle("导入中……");
//                                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//                                progressDialog.show();
//                            }
//                            //更新进度
//                            if (msg.what==0x07){
//                                Bundle data = msg.getData();
//                                int progress = data.getInt("progress");
//                                progressDialog.setProgress(progress);
//                            }
//                            //结束
//                            if (msg.what==0x06){
//                                ToastUtil.showShortToast("导入完成!");
//                                progressDialog.cancel();
//                            }
//                            //失败
//                            if (msg.what==0x08){
//                                ToastUtil.showShortToast("导入失败!");
//                                progressDialog.cancel();
//                            }
//                        }
//                    };
                    unzipFileToAPPStoragePath(realFilePath);
                }
            });
            alterDialog.show();
        }
    }

    private void unzipFileToAPPStoragePath(String filePath){
        ZIPUtils.unzipFile(filePath, MainActivity.APPStoragePath.replace("/files",""), new ZipListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void zipStart() {
                handler.sendEmptyMessage(0x05);
            }
            @Override
            public void zipSuccess() {
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