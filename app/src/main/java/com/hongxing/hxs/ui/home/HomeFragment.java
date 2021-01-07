package com.hongxing.hxs.ui.home;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.hongxing.hxs.R;
import com.hongxing.hxs.utils.CommonUtils;
import com.hongxing.hxs.utils.http.HttpUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class HomeFragment extends Fragment {
    private Runnable runnable;
    private Handler handler;
    private static List<String> url_list;

    @SuppressLint("HandlerLeak")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        ImageView imageView= root.findViewById(R.id.ad_img);
        AtomicReference<Bitmap> imgBM = new AtomicReference<>();
        handler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) { super.handleMessage(msg);
                try {
                    if (msg.what==0x00)
                        if (imgBM.get()!=null)
                            imageView.setImageBitmap(imgBM.get());
                }catch (Exception e){e.printStackTrace();}
            }
        };
        final Random random = new Random();
        runnable=new Runnable(){
            @Override
            public void run() {
                new Thread(()->{
                    try {
                        imgBM.set(BitmapFactory.decodeStream(
                                new URL(url_list.get(random.nextInt(url_list.size()))).openStream()));
                        handler.sendEmptyMessage(0x00);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                handler.postDelayed(this, 5500);
            }
        };
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();loadADURLList();
    }

    @Override
    public void onPause()  {
        super.onPause();
        handler.removeCallbacks(runnable);
    }

    private void loadADURLList(){
        if (url_list==null)
        HttpUtils.getADImagesURLListFile2Local(new HttpUtils.Listener() {
            @Override
            public void startFileTransfer() {
                System.out.println("getADImagesURLListFile2Local");
            }
            @Override
            public void success(String response) {
                File file = new File(CommonUtils.getDiskCachePath() + "/img-url-list.temp");
                if (file.exists()){
                    url_list=new ArrayList<>();
                    BufferedReader reader=null;
                    try {
                        reader = new BufferedReader(new FileReader(file));
                        String line;
                        while ((line=reader.readLine())!=null)
                            if (!"".equals(line)) url_list.add(line);
                        handler.post(runnable);
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }finally {
                        if (reader!=null)
                            try { reader.close(); } catch (IOException ignored) { }
                    }
                }
            }
            @Override
            public void error(Exception e) {
                e.printStackTrace();
            }
        });
        else handler.post(runnable);
    }

}