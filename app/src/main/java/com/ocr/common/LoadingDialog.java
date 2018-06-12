package com.ocr.common;

import android.app.Dialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;

import com.ocr.R;


public class LoadingDialog extends Dialog {

    public LoadingDialog(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        this.setOnKeyListener((dialog, keyCode, event) -> keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_BACK);
        this.setCanceledOnTouchOutside(false);
        this.setContentView(LayoutInflater.from(context).inflate(R.layout.loading_dialog, null));
        this.setCancelable(false);
    }
}
