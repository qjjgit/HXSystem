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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.hongxing.hxs.MainActivity;
import com.hongxing.hxs.R;
import com.hongxing.hxs.db.DBManager;
import com.hongxing.hxs.service.CrudService;
import com.hongxing.hxs.utils.zip.ZIPUtils;
import com.hongxing.hxs.utils.zip.ZipListener;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;


public class NotificationsFragment extends Fragment {

    private NotificationsViewModel notificationsViewModel;
    private TextView lastBackup;
    private Handler handler;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        notificationsViewModel =
                ViewModelProviders.of(this).get(NotificationsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);
        setClick(root);
        lastBackup = root.findViewById(R.id.text_lastBackup);
        notificationsViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                CrudService service = new CrudService(getContext());
                String date = service.getLastBackup();service.close();
                lastBackup.append(date);
            }
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
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss", Locale.CHINA);
                try {
                    long last = format.parse(lastBackup.getText().toString().replace("上一次备份：","")).getTime();
                    int minute = (int)(new Date().getTime() - last) / 60000;
                    if (minute<3){
                        Toast.makeText(context,"您刚刚进行过备份，请勿频繁操作！",Toast.LENGTH_SHORT).show();
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
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
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
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK ) {
            return;
        }
        if (requestCode==0x04){
            final Context context = getContext();
            String relativePath = data.getData().getPath();
            final String fileName = relativePath.substring(relativePath.lastIndexOf("/")+1);
            final String realFilePath=Environment.getExternalStorageDirectory().getPath()
                    +File.separator +"鸿兴系统"+File.separator+fileName;
            if (!fileName.contains("鸿兴系统")||!fileName.contains("备份")
                    ||!".zip".equals(fileName.substring(fileName.length() - 4))){
                Toast.makeText(context,"请选择正确的数据包！\n(数据包在 \"鸿兴超市\" 文件夹内)",Toast.LENGTH_LONG).show();
                return;
            }
            final ProgressDialog progressDialog = new ProgressDialog(context);
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
                                Toast.makeText(context,"导入完成!",Toast.LENGTH_SHORT).show();
                                progressDialog.cancel();
                            }
                            //失败
                            if (msg.what==0x08){
                                Toast.makeText(context,"导入失败!",Toast.LENGTH_SHORT).show();
                                progressDialog.cancel();
                            }
                        }
                    };
                    ZIPUtils.unzipFile(realFilePath, MainActivity.APPStoragePath.replace("/files",""), new ZipListener() {
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
            });
            alterDialog.show();
        }
    }
}