/*
 * Copyright (C) 2013 Andreas Stuetz <andreas.stuetz@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jia.blossom.viewpageindicator;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.jia.blossom.pageindicator.PagerSlidingTabStrip;


public class MainActivity extends FragmentActivity
{
    
    private final Handler handler=new Handler();
    private PagerSlidingTabStrip tabs;
    private ViewPager pager;
    private MyPagerAdapter adapter;
    private Drawable oldBackground=null;
    private int currentColor=0xFF666666;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tabs=(PagerSlidingTabStrip)findViewById(R.id.tabs);
        pager=(ViewPager)findViewById(R.id.pager);
        adapter=new MyPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        final int pageMargin=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        pager.setPageMargin(pageMargin);
        tabs.setViewPager(pager);
        tabs.setOnPageChangeListener(new OnPageChangeListener(){
            
            @Override
            public void onPageSelected(int position) {
            }
            
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }
            
            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        changeColor(currentColor);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_contact:
                QuickContactFragment dialog=new QuickContactFragment();
                dialog.show(getSupportFragmentManager(), "QuickContactFragment");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void changeColor(int newColor) {
        tabs.setIndicatorColor(newColor);
        if(Build.VERSION.SDK_INT >= 11) {
            Drawable colorDrawable=new ColorDrawable(newColor);
            Drawable bottomDrawable=getResources().getDrawable(R.drawable.actionbar_bottom);
            LayerDrawable ld=new LayerDrawable(new Drawable[]{colorDrawable,bottomDrawable});
            if(oldBackground == null) {
                if(Build.VERSION.SDK_INT < 17) {
                    Log.e("lintest", Build.VERSION.SDK_INT + " < 17");
                    ld.setCallback(drawableCallback);
                }
                else{
                    getActionBar().setBackgroundDrawable(ld);
                }
            }
            else{
                // 一个TransitionDrawable是一个特殊的Drawable对象，可以实现两个drawable资源之间淡入淡出的效果。
                TransitionDrawable td=new TransitionDrawable(new Drawable[]{oldBackground,ld});
                if(Build.VERSION.SDK_INT < 17) {
                    td.setCallback(drawableCallback);
                }
                else{
                    getActionBar().setBackgroundDrawable(td);
                }
                td.startTransition(3000);
            }
            oldBackground=ld;
            getActionBar().setDisplayShowTitleEnabled(false);
            getActionBar().setDisplayShowTitleEnabled(true);
        }
        currentColor=newColor;
    }
    
    public void onColorClicked(View v) {
        int color=Color.parseColor(v.getTag().toString());
        changeColor(color);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentColor", currentColor);
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentColor=savedInstanceState.getInt("currentColor");
        changeColor(currentColor);
    }
    
    private Drawable.Callback drawableCallback=new Drawable.Callback(){
        
        @Override
        public void invalidateDrawable(Drawable who) {
            getActionBar().setBackgroundDrawable(who);
            Log.w("lintest", "invalidateDrawable");
        }
        
        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            handler.postAtTime(what, when);
            Log.i("lintest", "scheduleDrawable");
        }
        
        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            handler.removeCallbacks(what);
            Log.d("lintest", "unscheduleDrawable");
        }
    };
    
    public class MyPagerAdapter extends FragmentPagerAdapter
    {
        
        private final String[] TITLES={"Categories","Home","Top Paid","Top Free","Top Grossing","Top New Paid","Top New Free","Trending"};
        
        public MyPagerAdapter(FragmentManager fm){
            super(fm);
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }
        
        @Override
        public int getCount() {
            return TITLES.length;
        }
        
        @Override
        public Fragment getItem(int position) {
            return SuperAwesomeCardFragment.newInstance(position);
        }
    }
}