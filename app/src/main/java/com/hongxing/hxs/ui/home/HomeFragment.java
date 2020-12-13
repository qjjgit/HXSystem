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
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.hongxing.hxs.R;
import com.hongxing.hxs.utils.CommonUtils;

import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView textView = root.findViewById(R.id.text_home);
        homeViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
                AtomicReference<Bitmap> pngBM = new AtomicReference<>();
                ImageView imageView = root.findViewById(R.id.ad_img);
                @SuppressLint("HandlerLeak")
                Handler handler = new Handler() {
                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        super.handleMessage(msg);
                        try {
                            if (msg.what==0x00)
                                if (imageView==null) System.out.println("imgV is null");
                                else if (pngBM.get()!=null)
                                    imageView.setImageBitmap(pngBM.get());
                                else System.out.println("pngBM null");
                        }catch (Exception e){e.printStackTrace();}
                    }
                };
                new Thread(() -> {
                    try {
                        pngBM.set(BitmapFactory.decodeStream(
                                new URL(CommonUtils.SERVERADDRESS+"/getImg").openStream()));
                        handler.sendEmptyMessage(0x00);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });
        return root;
    }
}