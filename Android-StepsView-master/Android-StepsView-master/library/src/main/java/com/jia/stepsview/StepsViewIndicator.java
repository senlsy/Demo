package com.jia.stepsview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class StepsViewIndicator extends View
{
    
    public interface OnDrawListener
    {
        
        public void onReady();
    }
    
    private static final int THUMB_SIZE = 70;// 节点直徑
    private Paint paint = new Paint();
    private Paint selectedPaint = new Paint();
    private int mNumOfStep;// 节点数
    private float mLineHeight;// 中间线的高度
    private float mThumbRadius;
    private float mCircleRadius;
    private float mPadding;
    private int mProgressColor = Color.YELLOW;
    private int mBarColor = Color.BLACK;
    private float mCenterY;// 节点Y轴位置
    private float mLeftX;// 最左边距
    private float mRightX;// 最右边距
    private float mLeftY;// 线top位置
    private float mRightY;// 线buttom位置
    private float mDelta;
    private List<Float> mThumbContainerXPosition = new ArrayList<>();// 节点数
    private int mCompletedPosition;// 完成的节点
    private OnDrawListener mDrawListener;// 节点间距
    
    public StepsViewIndicator(Context context){
        this(context, null);
    }
    
    public StepsViewIndicator(Context context, AttributeSet attrs){
        this(context, attrs, 0);
    }
    
    public StepsViewIndicator(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StepsViewIndicator);
        mNumOfStep = a.getInt(R.styleable.StepsViewIndicator_numOfSteps, 2);
        mLineHeight = a.getDimension(R.styleable.StepsViewIndicator_lineHight, 1);
        a.recycle();
        init();
    }
    
    private void init() {
        mThumbRadius = 0.4f * THUMB_SIZE;// 内圆半径
        mCircleRadius = 0.7f * mThumbRadius;// 外圆半径
        mPadding = 0.5f * THUMB_SIZE;
    }
    
    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Log.e("lintest", "onSizeChanged");
        // layoutPosition();
    }
    
    private void numOfStepChange()
    {
        mCenterY = 0.5f * getHeight();
        mLeftX = mPadding;
        mRightX = getWidth() - mPadding;
        mLeftY = mCenterY - (mLineHeight / 2);
        mRightY = 0.5f * (getHeight() + mLineHeight);
        //
        // 节点中心位置
        mDelta = (mRightX - mLeftX) / (mNumOfStep - 1);
        mThumbContainerXPosition.clear();
        mThumbContainerXPosition.add(mLeftX);
        for(int i = 1;i < mNumOfStep - 1;i++){
            mThumbContainerXPosition.add(mLeftX + (i * mDelta));
        }
        mThumbContainerXPosition.add(mRightX);
        if(mDrawListener != null)
            mDrawListener.onReady();
    }
    
    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Log.e("lintest", "onMeasure");
        int width = 200;
        if(MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)){
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        int height = THUMB_SIZE + 20;
        if(MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)){
            height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
        }
        setMeasuredDimension(width, height);
    }
    
    @Override
    protected synchronized void onDraw(Canvas canvas) {
        // Log.e("lintest", "onDraw");
        numOfStepChange();
        // Draw rect bounds
        paint.setAntiAlias(true);
        paint.setColor(mBarColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        selectedPaint.setAntiAlias(true);
        selectedPaint.setColor(mProgressColor);
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(2);
        // 画外圆边框
        for(int i = 0;i < mThumbContainerXPosition.size();i++){
            canvas.drawCircle(mThumbContainerXPosition.get(i), mCenterY, mCircleRadius, (i <= mCompletedPosition) ? selectedPaint : paint);
        }
        paint.setStyle(Paint.Style.FILL);
        selectedPaint.setStyle(Paint.Style.FILL);
        // 画线
        for(int i = 0;i < mThumbContainerXPosition.size() - 1;i++){
            final float pos = mThumbContainerXPosition.get(i) + mCircleRadius;
            final float pos2 = mThumbContainerXPosition.get(i + 1) - mCircleRadius;
            canvas.drawRect(pos, mLeftY, pos2, mRightY, (i < mCompletedPosition) ? selectedPaint : paint);
        }
        // 画实心内圓
        for(int i = 0;i < mThumbContainerXPosition.size();i++){
            final float pos = mThumbContainerXPosition.get(i);
            if(i <= mCompletedPosition)
                canvas.drawCircle(pos, mCenterY, mCircleRadius * 0.7f, selectedPaint);
        }
        // Draw rest of circle
        // for(int i=0;i < mThumbContainerXPosition.size();i++){
        // final float pos=mThumbContainerXPosition.get(i);
        // canvas.drawCircle(pos, mCenterY, mCircleRadius, (i <=
        // mCompletedPosition) ? selectedPaint : paint);
        // if(i == mCompletedPosition) {
        // selectedPaint.setColor(getColorWithAlpha(mProgressColor, 0.2f));
        // canvas.drawCircle(pos, mCenterY, mCircleRadius * 1.8f,
        // selectedPaint);
        // }
        // }
    }
    
    public void setStepSize(int size) {
        mNumOfStep = size;
        invalidate();
    }
    
    public void setDrawListener(OnDrawListener drawListener) {
        mDrawListener = drawListener;
    }
    
    public List<Float> getThumbContainerXPosition() {
        return mThumbContainerXPosition;
    }
    
    public void setCompletedPosition(int position) {
        mCompletedPosition = position;
    }
    
    public void reset() {
        setCompletedPosition(0);
    }
    
    public void setProgressColor(int progressColor) {
        mProgressColor = progressColor;
    }
    
    public void setBarColor(int barColor) {
        mBarColor = barColor;
    }
    
    public static int getColorWithAlpha(int color, float ratio) {
        int newColor = 0;
        int alpha = Math.round(Color.alpha(color) * ratio);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        newColor = Color.argb(alpha, r, g, b);
        return newColor;
    }
}
