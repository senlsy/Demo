package com.jia.blossom.pageindicator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;


//实现原理：设置ViewPager的PageListener。在PageListener中监听page的偏移量，
//然后scrollTo自身HorizontalScrollView的偏移
public class PagerSlidingTabStrip extends HorizontalScrollView
{
    
    public interface IconTabProvider
    {
        
        // tab为图案，pagerAdapter应该实现的接口，返回图片的resid
        public int getPageIconResId(int position);
    }
    
    public interface TabClickListenner
    {
        
        public void onItemClick(View view, int position);
    }
    
    // android自身的属性
    private static final int[] ATTRS=new int[]{android.R.attr.textSize,android.R.attr.textColor};
    // @formatter:on
    private LinearLayout.LayoutParams defaultTabLayoutParams;// tab的width采用的参数width=WRAP_CONTENT
    private LinearLayout.LayoutParams expandedTabLayoutParams;// tab的width采用的参数width=0,weight=1
    private final PageListener pageListener=new PageListener();// OnPageChangeListener的子类，处理完自身所需的监听之后，把回调委托给delegatePageListener
    public OnPageChangeListener delegatePageListener;// 委托的OnPageChangeListener
    public TabClickListenner delegateTabClickListener;// tab点击的委托响应
    //
    private ViewPager pager;
    private int tabCount;// tab的个数
    private int currentPosition=0;// viewpager在哪个位置
    private float currentPositionOffset=0f;// viewpager偏移百分比
    private int lastScrollX=0;
    //
    private Paint rectPaint;// 指示器画笔
    private int indicatorColor=0xFF666666;// 指示器颜色
    private int underlineColor=0x1A000000;// 指示器下线的颜色
    private int indicatorHeight=8;// 指示器高度
    private int underlineHeight=2;// 指示器下线的高度
    //
    private Paint dividerPaint;// tab分隔线“|”的画笔
    private LinearLayout tabsContainer;// tab的容器viewgroup
    private int tabBackgroundResId=R.drawable.background_tab;// tab的背景
    private int dividerColor=0x1A000000;// tab间隔线“|”的颜色
    private boolean shouldExpand=false;// tab采用defaultTabLayoutParams还是expandedTabLayoutParams。
    private boolean textAllCaps=true;// 是否将tab的文字转换为大写
    private int scrollOffset=52;// 预留52个像素显示前面的tab
    private int dividerPadding=12;// tab分隔线“|”上下的padding
    private int tabPadding=24;// tab的左右pandding
    private int dividerWidth=1;// tab间隔线“|”的宽度
    private int tabTextSize=12;// tab文字大小
    private int tabTextColor=0xFF666666;// 文字颜色
    private int tabSelectedColor=0xFF666666;// 文字颜色
    private Typeface tabTypeface=null;
    private int tabTypefaceStyle=Typeface.NORMAL;
    private Locale locale;// 代表一种特定的语言和区域，向本地敏感的类提供本地化信息。
    
    public PagerSlidingTabStrip(Context context){
        this(context, null);
    }
    
    public PagerSlidingTabStrip(Context context, AttributeSet attrs){
        this(context, attrs, 0);
    }
    
    public PagerSlidingTabStrip(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        setFillViewport(true);// 是否拉伸内容来填充视图
        setWillNotDraw(false);// 如果不许绘制可以设置为true，优化性能
        // ------------------------------------
        tabsContainer=new LinearLayout(context);
        tabsContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabsContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(tabsContainer);
        // 初始化默认值
        DisplayMetrics dm=getResources().getDisplayMetrics();
        scrollOffset=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, scrollOffset, dm);
        indicatorHeight=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, indicatorHeight, dm);
        underlineHeight=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, underlineHeight, dm);
        dividerPadding=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dividerPadding, dm);
        tabPadding=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, tabPadding, dm);
        dividerWidth=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dividerWidth, dm);
        tabTextSize=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, tabTextSize, dm);
        // 获取xml设置的属性(android自身属性)
        // TypedArray a=context.obtainStyledAttributes(attrs, ATTRS);
        // tabTextSize=a.getDimensionPixelSize(0, tabTextSize);
        // tabTextColor=a.getColor(1, tabTextColor);
        // a.recycle();
        // 获取xml设置的属性(自定义的属性)
        TypedArray a=context.obtainStyledAttributes(attrs, R.styleable.PagerSlidingTabStrip);
        indicatorColor=a.getColor(R.styleable.PagerSlidingTabStrip_pstsIndicatorColor, indicatorColor);
        underlineColor=a.getColor(R.styleable.PagerSlidingTabStrip_pstsUnderlineColor, underlineColor);
        dividerColor=a.getColor(R.styleable.PagerSlidingTabStrip_pstsDividerColor, dividerColor);
        indicatorHeight=a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsIndicatorHeight, indicatorHeight);
        underlineHeight=a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsUnderlineHeight, underlineHeight);
        dividerPadding=a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsDividerPadding, dividerPadding);
        tabPadding=a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsTabPaddingLeftRight, tabPadding);
        tabBackgroundResId=a.getResourceId(R.styleable.PagerSlidingTabStrip_pstsTabBackground, tabBackgroundResId);
        shouldExpand=a.getBoolean(R.styleable.PagerSlidingTabStrip_pstsShouldExpand, shouldExpand);
        scrollOffset=a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsScrollOffset, scrollOffset);
        textAllCaps=a.getBoolean(R.styleable.PagerSlidingTabStrip_pstsTextAllCaps, textAllCaps);
        tabTextColor=a.getColor(R.styleable.PagerSlidingTabStrip_textColor, tabTextColor);
        tabSelectedColor=a.getColor(R.styleable.PagerSlidingTabStrip_tabSelectedColor, tabTextColor);
        tabTextSize=a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_tabTextSize, tabTextSize);
        tabTypefaceStyle=a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_tabTextStyle, tabTypefaceStyle);
        a.recycle();
        //
        rectPaint=new Paint();
        rectPaint.setAntiAlias(true);
        rectPaint.setStyle(Style.FILL);
        dividerPaint=new Paint();
        dividerPaint.setAntiAlias(true);
        dividerPaint.setStrokeWidth(dividerWidth);
        defaultTabLayoutParams=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        expandedTabLayoutParams=new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);
        if(locale == null) {
            locale=getResources().getConfiguration().locale;
        }
    }
    
    // ------------------------------------------------
    public void setViewPager(ViewPager pager) {
        this.pager=pager;
        if(pager.getAdapter() == null) {
            throw new IllegalStateException("ViewPager does not have adapter instance.");
        }
        pager.setOnPageChangeListener(pageListener);
        notifyDataSetChanged();
    }
    
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.delegatePageListener=listener;
    }
    
    public void notifyDataSetChanged() {
        tabsContainer.removeAllViews();
        tabCount=pager.getAdapter().getCount();
        for(int i=0;i < tabCount;i++){
            if(pager.getAdapter() instanceof IconTabProvider) {
                addIconTab(i, ((IconTabProvider)pager.getAdapter()).getPageIconResId(i));
            }
            else{
                addTextTab(i, pager.getAdapter().getPageTitle(i).toString());
            }
        }
        updateTabStyles();
        getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener(){
            
            @SuppressWarnings("deprecation")
            @SuppressLint("NewApi")
            @Override
            public void onGlobalLayout() {
                Log.e("lintest", "onGlobalLayout");
                // 视图树布局发生变化！
                if(Build.VERSION.SDK_INT < 16) {
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                else{
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                currentPosition=pager.getCurrentItem();
                scrollToChild(currentPosition, 0);
            }
        });
    }
    
    private void addTextTab(final int position, String title) {
        TextView tab=new TextView(getContext());
        tab.setText(title);
        tab.setGravity(Gravity.CENTER);
        tab.setSingleLine();
        addTab(position, tab);
    }
    
    private void addIconTab(final int position, int resId) {
        ImageButton tab=new ImageButton(getContext());
        tab.setImageResource(resId);
        addTab(position, tab);
    }
    
    private void addTab(final int position, View tab) {
        tab.setFocusable(true);
        tab.setOnClickListener(new OnClickListener(){
            
            @Override
            public void onClick(View v) {
                pager.setCurrentItem(position);
                if(delegateTabClickListener != null)
                    delegateTabClickListener.onItemClick(v, position);
            }
        });
        tab.setPadding(tabPadding, 0, tabPadding, 0);
        tabsContainer.addView(tab, position, shouldExpand ? expandedTabLayoutParams : defaultTabLayoutParams);
    }
    
    private void updateTabStyles() {
        for(int i=0;i < tabCount;i++){
            View v=tabsContainer.getChildAt(i);
            v.setBackgroundResource(tabBackgroundResId);
            if(v instanceof TextView) {
                TextView tab=(TextView)v;
                tab.setTextSize(TypedValue.COMPLEX_UNIT_PX, tabTextSize);
                tab.setTypeface(tabTypeface, tabTypefaceStyle);
                tab.setTextColor(tabTextColor);
                if(textAllCaps) {
                    if(Build.VERSION.SDK_INT >= 14) {
                        tab.setAllCaps(true);
                    }
                    else{
                        tab.setText(tab.getText().toString().toUpperCase(locale));
                    }
                }
            }
        }
    }
    
    // ------------------------------------
    private class PageListener implements OnPageChangeListener
    {
        
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // 参数1：当前偏移的页面
            // 参数2：当前页面偏移的百分比
            // 参数3:当前页面偏移的像素
            // Log.w("lintest", "onPageScrollStateChanged---" + position + "," +
            // positionOffset + "%," + positionOffsetPixels);
            currentPosition=position;
            currentPositionOffset=positionOffset;
            scrollToChild(position, (int)(positionOffset * tabsContainer.getChildAt(position).getWidth()));
            invalidate();
            if(delegatePageListener != null) {
                delegatePageListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        }
        
        @Override
        public void onPageScrollStateChanged(int state) {
            // state=0静止状态，1开始切换状态，2切换完毕
            // Log.i("lintest", "onPageScrollStateChanged---state=" + state);
            if(state == ViewPager.SCROLL_STATE_IDLE) {
                // viewpager静止
                scrollToChild(pager.getCurrentItem(), 0);
            }
            if(delegatePageListener != null) {
                delegatePageListener.onPageScrollStateChanged(state);
            }
        }
        
        @Override
        public void onPageSelected(int position) {
            // viewpager切换完毕后回调
            // Log.e("lintest", "onPageSelected---position=" + position);
            if(delegatePageListener != null) {
                delegatePageListener.onPageSelected(position);
            }
        }
    }
    
    private TextView currentTab;
    
    private void scrollToChild(int position, int offset) {
        if(tabCount == 0) {
            return;
        }
        View temp=tabsContainer.getChildAt(position);
        // tabsContainer.getChildAt(position).getLeft()是tab相对于父布局的左坐标！
        int newScrollX=temp.getLeft() + offset;
        // Log.e("lintest","tab
        // left="+tabsContainer.getChildAt(position).getLeft()+",offset="+offset);
        if(offset == 0 && (temp instanceof TextView)) {
            TextView tv=(TextView)temp;
            if(currentTab != temp) {
                tv.setTextColor(tabSelectedColor);
                if(currentTab != null) {
                    currentTab.setTextColor(tabTextColor);
                }
                currentTab=tv;
            }
        }
        if(position > 0 || offset > 0) {
            newScrollX-=scrollOffset;
        }
        if(newScrollX != lastScrollX) {
            lastScrollX=newScrollX;
            scrollTo(newScrollX, 0);
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(isInEditMode() || tabCount == 0) {
            return;
        }

        // 当前page对应的tab位置
        View currentTab=tabsContainer.getChildAt(currentPosition);
        float lineLeft=currentTab.getLeft();
        float lineRight=currentTab.getRight();

        // 下一个tab位置
        if(currentPositionOffset > 0f && currentPosition < tabCount - 1) {
            View nextTab=tabsContainer.getChildAt(currentPosition + 1);
            final float nextTabLeft=nextTab.getLeft();
            final float nextTabRight=nextTab.getRight();
            lineLeft=(currentPositionOffset * nextTabLeft + (1f - currentPositionOffset) * lineLeft);
            lineRight=(currentPositionOffset * nextTabRight + (1f - currentPositionOffset) * lineRight);
        }

        final int height=getHeight();
        rectPaint.setColor(indicatorColor);
        // 画指标
        canvas.drawRect(lineLeft, height - indicatorHeight, lineRight, height, rectPaint);


        // 画指标下面的线
        rectPaint.setColor(underlineColor);
        canvas.drawRect(0, height - underlineHeight, tabsContainer.getWidth(), height, rectPaint);
        // 画tab的间隔线“|”
        dividerPaint.setColor(dividerColor);
        for(int i=0;i < tabCount - 1;i++){// 画在tab的右边，最后一个tab不用画。
            View tab=tabsContainer.getChildAt(i);
            canvas.drawLine(tab.getRight(), dividerPadding, tab.getRight(), height - dividerPadding, dividerPaint);
        }
    }
    
    // ------------------------------------------
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState=(SavedState)state;
        super.onRestoreInstanceState(savedState.getSuperState());
        currentPosition=savedState.currentPosition;
        requestLayout();
    }
    
    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState=super.onSaveInstanceState();
        SavedState savedState=new SavedState(superState);
        savedState.currentPosition=currentPosition;
        return savedState;
    }
    
    static class SavedState extends BaseSavedState
    {
        
        int currentPosition;
        
        public SavedState(Parcelable superState){
            super(superState);
        }
        
        private SavedState(Parcel in){
            super(in);
            currentPosition=in.readInt();
        }
        
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(currentPosition);
        }
        
        public static final Parcelable.Creator<SavedState> CREATOR=new Parcelable.Creator<SavedState>(){
            
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }
            
            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
    
    // ----------------------------------------------
    public void setIndicatorColor(int indicatorColor) {
        this.indicatorColor=indicatorColor;
        invalidate();
    }
    
    public void setIndicatorColorResource(int resId) {
        this.indicatorColor=getResources().getColor(resId);
        invalidate();
    }
    
    public int getIndicatorColor() {
        return this.indicatorColor;
    }
    
    public void setIndicatorHeight(int indicatorLineHeightPx) {
        this.indicatorHeight=indicatorLineHeightPx;
        invalidate();
    }
    
    public int getIndicatorHeight() {
        return indicatorHeight;
    }
    
    public void setUnderlineColor(int underlineColor) {
        this.underlineColor=underlineColor;
        invalidate();
    }
    
    public void setUnderlineColorResource(int resId) {
        this.underlineColor=getResources().getColor(resId);
        invalidate();
    }
    
    public int getUnderlineColor() {
        return underlineColor;
    }
    
    public void setDividerColor(int dividerColor) {
        this.dividerColor=dividerColor;
        invalidate();
    }
    
    public void setDividerColorResource(int resId) {
        this.dividerColor=getResources().getColor(resId);
        invalidate();
    }
    
    public int getDividerColor() {
        return dividerColor;
    }
    
    public void setUnderlineHeight(int underlineHeightPx) {
        this.underlineHeight=underlineHeightPx;
        invalidate();
    }
    
    public int getUnderlineHeight() {
        return underlineHeight;
    }
    
    public void setDividerPadding(int dividerPaddingPx) {
        this.dividerPadding=dividerPaddingPx;
        invalidate();
    }
    
    public int getDividerPadding() {
        return dividerPadding;
    }
    
    public void setScrollOffset(int scrollOffsetPx) {
        this.scrollOffset=scrollOffsetPx;
        invalidate();
    }
    
    public int getScrollOffset() {
        return scrollOffset;
    }
    
    public void setShouldExpand(boolean shouldExpand) {
        this.shouldExpand=shouldExpand;
        requestLayout();
    }
    
    public boolean getShouldExpand() {
        return shouldExpand;
    }
    
    public boolean isTextAllCaps() {
        return textAllCaps;
    }
    
    public void setAllCaps(boolean textAllCaps) {
        this.textAllCaps=textAllCaps;
        updateTabStyles();
    }
    
    public void setTextSize(int textSizePx) {
        this.tabTextSize=textSizePx;
        updateTabStyles();
    }
    
    public int getTextSize() {
        return tabTextSize;
    }
    
    public void setTextColor(int textColor) {
        this.tabTextColor=textColor;
        updateTabStyles();
    }
    
    public void setTextColorResource(int resId) {
        this.tabTextColor=getResources().getColor(resId);
        updateTabStyles();
    }
    
    public int getTextColor() {
        return tabTextColor;
    }
    
    public void setTypeface(Typeface typeface, int style) {
        this.tabTypeface=typeface;
        this.tabTypefaceStyle=style;
        updateTabStyles();
    }
    
    public void setTabBackground(int resId) {
        this.tabBackgroundResId=resId;
        updateTabStyles();
    }
    
    public int getTabBackground() {
        return tabBackgroundResId;
    }
    
    public void setTabPaddingLeftRight(int paddingPx) {
        this.tabPadding=paddingPx;
        updateTabStyles();
    }
    
    public int getTabPaddingLeftRight() {
        return tabPadding;
    }
}
