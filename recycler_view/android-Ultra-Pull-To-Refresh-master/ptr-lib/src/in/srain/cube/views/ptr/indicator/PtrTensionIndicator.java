package in.srain.cube.views.ptr.indicator;

/**
 * 张力效果的计算器
 */
public class PtrTensionIndicator extends PtrIndicator {

    private float DRAG_RATE = 0.5f;//张力系数

    private float mDownY;//手指down下的y轴坐标
    private float mDownPos;//手指down下的位置
    private int mReleasePos;//手指释放的位置

    /**
     * 组成张力动画缩放的百分比
     */
    private float mOneHeight = 0;//head高度的5分之4
    private float mCurrentDragPercent;//当前下拉的距离与mOneHeight的百分比
    private float mReleasePercent = -1;//手指释放时下拉的距离与head高度的百分比

    /**
     * 设置head的高度，并设置mOneHeight=head高度的五分之四，mOneHeight用来计算 当前下拉距离与mOneHeight的百分比。
     */
    @Override
    public void setHeaderHeight(int height) {
        super.setHeaderHeight(height);
        mOneHeight = height * 4f / 5;
    }

    /**
     * 获取刷新时head保持的高度。篡改成mOneHeight
     */
    @Override
    public int getOffsetToKeepHeaderWhileLoading() {
        return getOffsetToRefresh();
    }

    /**
     * 获取触发刷新阀值。篡改成mOneHeight
     */
    @Override
    public int getOffsetToRefresh() {
        return (int) mOneHeight;
    }

    /**
     * 手指down状态，初始化手指down下的位置，初始化手指down下的y轴坐标
     */
    @Override
    public void onPressDown(float x, float y) {
        super.onPressDown(x, y);
        mDownY = y;
        mDownPos = getCurrentPosY();
    }

    /**
     * 释放手指，设置手指释放的位置，设置手指释放时下拉的距离与head高度的百分比
     */
    @Override
    public void onRelease() {
        super.onRelease();
        mReleasePos = getCurrentPosY();
        mReleasePercent = mCurrentDragPercent;
    }

    /**
     * 刷新完成时，设置手指释放的位置，设置手指释放时下拉的距离与head高度的百分比
     */
    @Override
    public void onUIRefreshComplete() {
        mReleasePos = getCurrentPosY();
        mReleasePercent = getOverDragPercent();
    }


    /**
     * 当前move event的y轴坐标小于down event的y轴坐标，调用父类return。即上拉动作(对于手指来说)
     * 当前move event的y轴坐标大于等于down event的y轴坐标，即下拉动作(对于手指来说)：
     * 1、当前下拉的距离与mOneHeight的百分比，即张力动画缩放的百分比
     * 2、设置event与event之前的xy轴偏移量到父类
     */
    @Override
    protected void processOnMove(float currentX, float currentY, float offsetX, float offsetY) {

        if (currentY < mDownY) {
            super.processOnMove(currentX, currentY, offsetX, offsetY);
            return;
        }

        // distance from top
        final float scrollTop = (currentY - mDownY) * DRAG_RATE + mDownPos;
        final float currentDragPercent = scrollTop / mOneHeight;
        if (currentDragPercent < 0) {
            setOffset(offsetX, 0);
            return;
        }

        mCurrentDragPercent = currentDragPercent;

        // 0 ~ 1
        float boundedDragPercent = Math.min(1f, Math.abs(currentDragPercent));
        float extraOS = scrollTop - mOneHeight;

        // 0 ~ 2
        // if extraOS lower than 0, which means scrollTop lower than onHeight, tensionSlingshotPercent will be 0.
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, mOneHeight * 2) / mOneHeight);
        float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow((tensionSlingshotPercent / 4), 2)) * 2f;
        float extraMove = (mOneHeight) * tensionPercent / 2;
        int targetY = (int) ((mOneHeight * boundedDragPercent) + extraMove);
        int change = targetY - getCurrentPosY();

        setOffset(currentX, change);

    }

    private float offsetToTarget(float scrollTop) {

        // distance from top
        final float currentDragPercent = scrollTop / mOneHeight;

        mCurrentDragPercent = currentDragPercent;

        // 0 ~ 1
        float boundedDragPercent = Math.min(1f, Math.abs(currentDragPercent));
        float extraOS = scrollTop - mOneHeight;

        // 0 ~ 2
        // if extraOS lower than 0, which means scrollTop lower than mOneHeight, tensionSlingshotPercent will be 0.
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, mOneHeight * 2) / mOneHeight);

        float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow((tensionSlingshotPercent / 4), 2)) * 2f;
        float extraMove = (mOneHeight) * tensionPercent / 2;
        int targetY = (int) ((mOneHeight * boundedDragPercent) + extraMove);

        return 0;
    }


    /**
     * 触摸状态下：返回当前下拉距离与head高的百分比
     * 非触摸状态下：
     */
    public float getOverDragPercent() {
        if (isUnderTouch()) {
            return mCurrentDragPercent;
        } else {
            if (mReleasePercent <= 0) {
                //当前位置与mOneHeight的百分比
                return 1.0f * getCurrentPosY() / getOffsetToKeepHeaderWhileLoading();
            }
            // after release
            return mReleasePercent * getCurrentPosY() / mReleasePos;
        }
    }
}
