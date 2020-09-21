package com.hongxing.hxs.ui.dashboard;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.hongxing.hxs.MainActivity;
import com.hongxing.hxs.R;
import com.hongxing.hxs.entity.Goods;
import com.hongxing.hxs.service.CrudService;
import com.hongxing.hxs.utils.GoodsUtils;

import java.util.Arrays;
import java.util.HashMap;
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
        setOnClick(title);
        title=relativeLayout.findViewById(R.id.list_1_2);
        title.setText(tableHeaderTexts[1]);
        title.setTextColor(Color.BLUE);
        setOnClick(title);
        title=relativeLayout.findViewById(R.id.list_1_3);
        title.setText(tableHeaderTexts[2]);
        title.setTextColor(Color.BLUE);
        setOnClick(title);
        title=relativeLayout.findViewById(R.id.list_1_4);
        title.setText(tableHeaderTexts[3]);
        title.setTextColor(Color.BLUE);
        setOnClick(title);
        title=relativeLayout.findViewById(R.id.list_1_5);
        title.setText(tableHeaderTexts[4]);
        title.setTextColor(Color.BLUE);
        setOnClick(title);
        /*表头*/
        tableHeader.addView(relativeLayout);
        CrudService service = new CrudService(getContext());
        final List<Goods> list = service.findByPage(0, service.getCount());
        service.close();
        for(int i=0;i<list.size();i++){
            relativeLayout=(RelativeLayout) LayoutInflater.from(this.getContext()).inflate(R.layout.table,null);
            final int finalI = i;
            relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showGoodsInfoPage(list.get(finalI));
                }
            });
            int color = Color.parseColor("#ffffff");
            if (i%2!=0)
                color= Color.parseColor("#eeeeee");
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

    private void setOnClick(MyTableTextView view){
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()){
                    case R.id.list_1_1:
                        Toast.makeText(getContext(),"点击了序号",Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.list_1_2:
                        Toast.makeText(getContext(),"点击了名称",Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.list_1_3:
                        Toast.makeText(getContext(),"点击了单位",Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.list_1_4:
                        Toast.makeText(getContext(),"点击了售价",Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.list_1_5:
                        Toast.makeText(getContext(),"点击了进货价",Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }


    private void showGoodsInfoPage(final Goods goods){
        View view= LayoutInflater.from(getContext()).inflate(R.layout.dialog_show_goodsinfo_page, null);
        final TextView cancel =view.findViewById(R.id.showGoods_cancel);
        final TextView sure =view.findViewById(R.id.showGoods_sure);
        final EditText eText_name =view.findViewById(R.id.showGoods_name);
        final Spinner spinner_unit =view.findViewById(R.id.showGoods_unitList);
        final TextView tView_barcode=view.findViewById(R.id.showGoods_barcode);
        final EditText eText_price =view.findViewById(R.id.showGoods_price);
        final EditText eText_orig =view.findViewById(R.id.showGoods_orig);
        AlertDialog.Builder builder= new AlertDialog.Builder(getContext());
        final Dialog dialog= builder.create();
        dialog.show();
        dialog.getWindow().setContentView(view);
        dialog.setCancelable(false);
        eText_name.setText(goods.getName());
        eText_name.setSelection(goods.getName().length());
        tView_barcode.setText(goods.getBarcode());
        eText_price.setText(String.valueOf(goods.getPrice()));
        eText_orig.setText(String.valueOf(goods.getOrig()));
//        String[] unitList = view.getResources().getStringArray(R.array.unitArray);
//        String units = Arrays.toString(unitList).replaceAll(", ","")
//                .replace("[请选择","").replace("]","");
        String units="个包件箱条罐听瓶排支袋副提盒桶斤卷";
        spinner_unit.setSelection(units.indexOf(goods.getUnit())+1,true);
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
                dialog.dismiss();
            }
        });
        sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Goods goodsCheck = new Goods();
                goodsCheck.setName(eText_name.getText().toString());
                goodsCheck.setBarcode(tView_barcode.getText().toString());
                goodsCheck.setUnit(goods.getUnit());
                goodsCheck.setPrice(Float.valueOf(eText_price.getText().toString()));
                goodsCheck.setOrig(Float.valueOf(eText_orig.getText().toString()));
                /*没有改动直接关闭dialog*/
                if (goods.equals(goodsCheck)) dialog.dismiss();
                HashMap<String, String> map = new HashMap<>();
                map.put("name",goodsCheck.getName());
                map.put("barcode",goodsCheck.getBarcode());
                map.put("unit",goodsCheck.getUnit());
                map.put("price",goods.getPrice().toString());
                map.put("orig",goods.getOrig().toString());
                boolean ok = GoodsUtils.checkGoodsInfo(getContext(),goods,map,GoodsUtils.DO_UPDATE);
                if (ok) dialog.dismiss();
            }
        });
    }

}