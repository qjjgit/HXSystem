package com.hongxing.hxs.ui.dialog;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.hongxing.hxs.MainActivity;
import com.hongxing.hxs.R;
import com.hongxing.hxs.entity.Goods;

public class ScanResultDialog extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_scan_result);
        setDialogData(MainActivity.goods);
    }
    public void setDialogData(Goods goods){
        TextView name=findViewById(R.id.goodsName);
        name.setText(goods.getName());
        TextView bar=findViewById(R.id.barcode);
        bar.setText(goods.getBarcode());
        TextView unit=findViewById(R.id.unit);
        unit.setText(goods.getUnit());
        TextView price=findViewById(R.id.price);
        price.setText((goods.getPrice().toString()+"元"));
        TextView orig=findViewById(R.id.orig);
        orig.setText("****元");
    }


}
