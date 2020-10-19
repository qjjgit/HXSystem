package com.hongxing.hxs.ui.notifications;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.hongxing.hxs.R;
import com.hongxing.hxs.db.DBManager;

import java.io.File;

import static android.app.Activity.RESULT_OK;


public class NotificationsFragment extends Fragment {

    private NotificationsViewModel notificationsViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        notificationsViewModel =
                ViewModelProviders.of(this).get(NotificationsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);
        setClick(root);
        final TextView textView = root.findViewById(R.id.text_notifications);
        notificationsViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                try {
                    String filePath= getContext().getExternalFilesDir(null).getPath();
//                    Toast.makeText(getContext(),filePath,Toast.LENGTH_LONG).show();
                    textView.setText(filePath);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        return root;
    }

    private void setClick(View view){
        view.findViewById(R.id.btn_exportData).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String strdir = Environment.getExternalStorageDirectory().getPath()
                        + File.separator + "鸿兴系统";
                File file = new File(strdir);
                if (!file.exists()) file.mkdir();
                boolean success = DBManager.exportDBFileToDir(strdir);
                String msg = success ? "已备份到" + strdir : "备份失败！";
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
        view.findViewById(R.id.btn_importData).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/x-zip-compressed");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
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
            Uri uri = data.getData();
            Toast.makeText(getContext(), "文件路径："+ uri.getPath(), Toast.LENGTH_SHORT).show();
        }
    }
}