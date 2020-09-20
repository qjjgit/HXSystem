package com.hongxing.hxs.ui.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.hongxing.hxs.R;
import com.hongxing.hxs.entity.Goods;
import com.hongxing.hxs.service.CrudService;

import java.util.List;

public class DashboardFragment extends Fragment {

    private LinearLayout tableHeader;
    private LinearLayout tableBody;
    private RelativeLayout relativeLayout;
    private String[] tableHeaderTexts={"序号","商品名称","单位","售价","进货价"};

    private DashboardViewModel dashboardViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                ViewModelProviders.of(this).get(DashboardViewModel.class);
        final View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
//        final TextView textView = root.findViewById(R.id.text_dashboard);
        dashboardViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
            tableHeader=root.findViewById(R.id.MyTableHeader);
            tableBody=root.findViewById(R.id.MyTable);
            initData();
            }
        });
        return root;
    }
    private void initData(){
        relativeLayout=(RelativeLayout)LayoutInflater.from(this.getContext()).inflate(R.layout.table,null);
        MyTableTextView title=relativeLayout.findViewById(R.id.list_1_1);
        title.setText(tableHeaderTexts[0]);
        title.setTextColor(Color.BLUE);
        title=relativeLayout.findViewById(R.id.list_1_2);
        title.setText(tableHeaderTexts[1]);
        title.setTextColor(Color.BLUE);
        title=relativeLayout.findViewById(R.id.list_1_3);
        title.setText(tableHeaderTexts[2]);
        title.setTextColor(Color.BLUE);
        title=relativeLayout.findViewById(R.id.list_1_4);
        title.setText(tableHeaderTexts[3]);
        title.setTextColor(Color.BLUE);
        title=relativeLayout.findViewById(R.id.list_1_5);
        title.setText(tableHeaderTexts[4]);
        title.setTextColor(Color.BLUE);
        /*表头*/
        tableHeader.addView(relativeLayout);
        CrudService service = new CrudService(getContext());
        List<Goods> list = service.findByPage(0, service.getCount());
        service.close();
        for(int i=0;i<list.size();i++){
            int color = Color.parseColor("#ffffff");
            if (i%2!=0)
             color= Color.parseColor("#eeeeee");
            relativeLayout=(RelativeLayout) LayoutInflater.from(this.getContext()).inflate(R.layout.table,null);
            MyTableTextView txt=relativeLayout.findViewById(R.id.list_1_1);
            txt.setBackgroundColor(color);
            txt.setText(String.valueOf(list.get(i).getId()));
            txt=relativeLayout.findViewById(R.id.list_1_2);
            txt.setBackgroundColor(color);
            txt.setText(list.get(i).getName());
            txt=relativeLayout.findViewById(R.id.list_1_3);
            txt.setBackgroundColor(color);
            txt.setText(list.get(i).getUnit());
            txt=relativeLayout.findViewById(R.id.list_1_4);
            txt.setBackgroundColor(color);
            txt.setText(String.valueOf(list.get(i).getPrice()));
            txt=relativeLayout.findViewById(R.id.list_1_5);
            txt.setBackgroundColor(color);
            txt.setText(String.valueOf(list.get(i).getOrig()));
            tableBody.addView(relativeLayout);
        }
    }

}