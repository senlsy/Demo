package com.jia.blossom.ios_dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class AlertDialog
{
    
    private Context context;
    private Dialog dialog;
    private LinearLayout lLayout_bg;
    private TextView txt_title;
    private TextView txt_msg;
    private Button btn_neg;
    private Button btn_neu;
    private Button btn_pos;
    private ImageView img_line_left;
    private ImageView img_line_right;
    private Display display;
    private boolean showTitle = false;
    private boolean showMsg = false;
    private boolean showPosBtn = false;
    private boolean showNeuBtn = false;
    private boolean showNegBtn = false;
    
    public AlertDialog(Context context){
        this.context = context;
        WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
    }
    
    public AlertDialog builder() {
        // 获取Dialog布局
        View view = LayoutInflater.from(context).inflate(R.layout.view_alertdialog, null);
        // 获取自定义Dialog布局中的控件
        lLayout_bg = (LinearLayout)view.findViewById(R.id.lLayout_bg);
        txt_title = (TextView)view.findViewById(R.id.txt_title);
        txt_title.setVisibility(View.GONE);
        txt_msg = (TextView)view.findViewById(R.id.txt_msg);
        txt_msg.setVisibility(View.GONE);
        btn_neg = (Button)view.findViewById(R.id.btn_neg);
        btn_neg.setVisibility(View.GONE);
        btn_neu = (Button)view.findViewById(R.id.btn_neu);
        btn_neu.setVisibility(View.GONE);
        btn_pos = (Button)view.findViewById(R.id.btn_pos);
        btn_pos.setVisibility(View.GONE);
        img_line_left = (ImageView)view.findViewById(R.id.line_left);
        img_line_left.setVisibility(View.GONE);
        img_line_right = (ImageView)view.findViewById(R.id.line_right);
        img_line_right.setVisibility(View.GONE);
        // 定义Dialog布局和参数
        dialog = new Dialog(context, R.style.AlertDialogStyle);
        dialog.setContentView(view);
        // 调整dialog背景大小
        lLayout_bg.setLayoutParams(new FrameLayout.LayoutParams((int)(display.getWidth() * 0.85), LayoutParams.WRAP_CONTENT));
        return this;
    }
    
    public AlertDialog setTitle(String title) {
        showTitle = true;
        if("".equals(title)){
            txt_title.setText("标题");
        }
        else{
            txt_title.setText(title);
        }
        return this;
    }
    
    public AlertDialog setMsg(String msg) {
        showMsg = true;
        if("".equals(msg)){
            txt_msg.setText("内容");
        }
        else{
            txt_msg.setText(msg);
        }
        return this;
    }
    
    public AlertDialog setCancelable(boolean cancel) {
        dialog.setCancelable(cancel);
        return this;
    }
    
    public AlertDialog setPositiveButton(String text, final OnClickListener listener) {
        showPosBtn = true;
        if("".equals(text)){
            btn_pos.setText("确定");
        }
        else{
            btn_pos.setText(text);
        }
        btn_pos.setOnClickListener(new OnClickListener(){
            
            @Override
            public void onClick(View v) {
                listener.onClick(v);
                dialog.dismiss();
            }
        });
        return this;
    }
    
    public AlertDialog setNeutralButton(String text, final OnClickListener listener) {
        showNeuBtn = true;
        if("".equals(text)){
            btn_neu.setText("其他");
        }
        else{
            btn_neu.setText(text);
        }
        btn_neu.setOnClickListener(new OnClickListener(){
            
            @Override
            public void onClick(View v) {
                listener.onClick(v);
                dialog.dismiss();
            }
        });
        return this;
    }
    
    public AlertDialog setNegativeButton(String text, final OnClickListener listener) {
        showNegBtn = true;
        if("".equals(text)){
            btn_neg.setText("取消");
        }
        else{
            btn_neg.setText(text);
        }
        btn_neg.setOnClickListener(new OnClickListener(){
            
            @Override
            public void onClick(View v) {
                listener.onClick(v);
                dialog.dismiss();
            }
        });
        return this;
    }
    
    private void setLayout() throws Exception {
        if( !showTitle && !showMsg){
            txt_title.setText("提示");
            txt_title.setVisibility(View.VISIBLE);
        }
        if(showTitle){
            txt_title.setVisibility(View.VISIBLE);
        }
        if(showMsg){
            txt_msg.setVisibility(View.VISIBLE);
        }
        // 要显示中间按钮，必须左右两边按钮都显示
        if(showNeuBtn && ( !showNegBtn || !showPosBtn))
        {
            throw new Exception("negbtn and posbtn must be visible when netbug visibel");
        }
        if(showNeuBtn)
        {
            btn_neu.setVisibility(View.VISIBLE);
            btn_neu.setBackgroundResource(R.drawable.alertdialog_middle_selector);
            img_line_right.setVisibility(View.VISIBLE);
        }
        if( !showPosBtn && !showNegBtn){
            btn_pos.setText("确定");
            btn_pos.setVisibility(View.VISIBLE);
            btn_pos.setBackgroundResource(R.drawable.alertdialog_single_selector);
            btn_pos.setOnClickListener(new OnClickListener(){
                
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }
        else if(showPosBtn && showNegBtn){
            btn_pos.setVisibility(View.VISIBLE);
            btn_pos.setBackgroundResource(R.drawable.alertdialog_right_selector);
            btn_neg.setVisibility(View.VISIBLE);
            btn_neg.setBackgroundResource(R.drawable.alertdialog_left_selector);
            img_line_left.setVisibility(View.VISIBLE);
        }
        else if(showPosBtn && !showNegBtn){
            btn_pos.setVisibility(View.VISIBLE);
            btn_pos.setBackgroundResource(R.drawable.alertdialog_single_selector);
        }
        else if( !showPosBtn && showNegBtn){
            btn_neg.setVisibility(View.VISIBLE);
            btn_neg.setBackgroundResource(R.drawable.alertdialog_single_selector);
        }
    }
    
    public void show() {
        try{
            setLayout();
        } catch(Exception e){
            e.printStackTrace();
        }
        dialog.show();
    }
}
