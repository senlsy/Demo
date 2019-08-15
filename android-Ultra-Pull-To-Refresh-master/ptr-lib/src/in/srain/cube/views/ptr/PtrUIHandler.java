package in.srain.cube.views.ptr;

import in.srain.cube.views.ptr.indicator.PtrIndicator;

/**
 * headview的状态回调
 */
public interface PtrUIHandler {

    /**
     * 通知UI动画回到初始状态
     */
    void onUIReset(PtrFrameLayout frame);

    /**
     * 通知UI动画准备下拉刷新
     */
    void onUIRefreshPrepare(PtrFrameLayout frame);

    /**
     * 通知UI动画刷新开始
     */
    void onUIRefreshBegin(PtrFrameLayout frame);

    /**
     * 通知UI动画刷新完成
     */
    void onUIRefreshComplete(PtrFrameLayout frame);

    /**
     * 通知UI动画，下拉上滑位置变化
     *
     * @param frame
     * @param isUnderTouch 是否处于触摸状态下
     * @param status       1：初始化、 2：准备下拉、 3：刷新中、 4：刷新完成
     * @param ptrIndicator 计算器，可以拿到位置变化的各种数据
     */
    void onUIPositionChange(PtrFrameLayout frame, boolean isUnderTouch, byte status, PtrIndicator ptrIndicator);
}
