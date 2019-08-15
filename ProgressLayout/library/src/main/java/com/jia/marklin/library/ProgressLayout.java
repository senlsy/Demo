package com.jia.marklin.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

public class ProgressLayout extends FrameLayout {

    public interface Callback {
        /*该函数是执行在mian线程上，不建议耗时处理*/
        void showing(ShowType TYPE, View tagetView);
    }

    private int resId_progress;
    private int resId_error;
    private int resId_empty;
    //
    private View progress_container;
    private View error_container;
    private View empty_container;
    private View content_container;
    private View currenView;
    private Callback callback;

    public enum ShowType {
        PROGRESS, ERROR, EMPTY, CONTENT;
    }

    public Callback getCallback() {
        return callback;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    //
    public ProgressLayout(Context context) {
        super(context);
    }

    public ProgressLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProgressLayout);
        resId_progress = a.getResourceId(R.styleable.ProgressLayout_progress_Layout, R.layout.progress_container);
        resId_error = a.getResourceId(R.styleable.ProgressLayout_error_Layout, R.layout.error_container);
        resId_empty = a.getResourceId(R.styleable.ProgressLayout_empty_Layout, R.layout.empty_container);
        a.recycle();
        init(resId_progress, resId_error, resId_empty);
    }

    private void init(int resId_progress, int resId_error, int resId_empty) {
        progress_container = LayoutInflater.from(getContext()).inflate(resId_progress, null);
        this.addView(progress_container);
        currenView = progress_container;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        content_container = this.getChildAt(getChildCount() - 1);
        if (content_container != null)
            content_container.setVisibility(View.GONE);
    }

    /* 显示你布局的内容 */
    public void showContent() {
        showContentWithAnimate(true);
    }

    public void showContentWithAnimate(boolean animate) {
        showView(ShowType.CONTENT, content_container, animate);
    }

    /* 显示进度条页面 */
    public void showProgress() {
        showProgressWithAnimate(true);
    }

    public void showProgressWithAnimate(boolean animate) {
        showView(ShowType.PROGRESS, progress_container, animate);
    }

    /* 显示空数据页面 */
    public void showEmpty() {
        showEmptyWithAnimate(true);
    }

    public void showEmptyWithAnimate(boolean animate) {
        if (empty_container == null) {
            empty_container = LayoutInflater.from(getContext()).inflate(resId_empty, null);
            this.addView(empty_container);
        }
        showView(ShowType.EMPTY, empty_container, animate);
    }

    /* 现实错误页面 */
    public void showError() {
        showErrorWithAnimate(true);
    }

    public void showErrorWithAnimate(boolean animate) {
        if (error_container == null) {
            error_container = LayoutInflater.from(getContext()).inflate(resId_error, null);
            this.addView(error_container);
        }
        showView(ShowType.ERROR, error_container, animate);
    }

    private void showView(ShowType type, View tagetView, boolean animate) {
        if (tagetView == null || tagetView == currenView)
            return;
        currenView.clearAnimation();
        tagetView.clearAnimation();
        if (animate) {
            currenView.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
            tagetView.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
        }
        currenView.setVisibility(View.GONE);
        tagetView.setVisibility(View.VISIBLE);
        currenView = tagetView;
        if (callback != null)
            callback.showing(type, tagetView);
    }
}
