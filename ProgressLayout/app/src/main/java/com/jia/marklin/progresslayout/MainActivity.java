package com.jia.marklin.progresslayout;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.jia.marklin.library.ProgressLayout;


public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    
    ProgressLayout layout;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layout=(ProgressLayout)this.findViewById(R.id.progresslayout);
    }
    
    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.button:
                layout.showContentWithAnimate(true);
                break;
            case R.id.button2:
                layout.showEmptyWithAnimate(true);
                break;
            case R.id.button3:
                layout.showErrorWithAnimate(true);
                break;
            case R.id.button4:
                layout.showProgressWithAnimate(true);
                break;
        }
    }
}
