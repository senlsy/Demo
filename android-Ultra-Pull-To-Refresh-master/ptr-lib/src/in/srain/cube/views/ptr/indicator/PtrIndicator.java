package in.srain.cube.views.ptr.indicator;

import android.graphics.PointF;

/**
 * PtrFrameLayout下拉刷新功能的计算器<br>
 * 1、记录down位置<br>、记录手指是否处于触摸状态下<br>
 * 2、记录event与上一个event之间的xy轴偏移量
 * 3、记录手指滑动的距离，相对于POS_START位置。 <br>
 * 4、通过mCurrentPos，判断：刚抵达某个位置、刚离开某个位置、是否超出某个位置、已经离开某个位置
 * 5、通过mCurrentPos
 */
public class PtrIndicator {


    /**
     * 临界点变量
     */
    private int mHeaderHeight;//head的高度
    protected int mOffsetToRefresh = 0;//触发刷新阀值(既下拉多少距离触发刷新)
    private float mRatioOfHeaderHeightToRefresh = 1.2f;//head高度与触发刷新阀值的比例

    private int mOffsetToKeepHeaderWhileLoading = -1;//刷新时head保持的高度，没设置值时，默认为head的高度。
    private int mRefreshCompleteY = 0;//记录刷新完成时的位置

    /**
     * 记录move与上一个move之间的偏移量
     */
    private float mOffsetX;//上一个event与当前event之间的x轴偏移量
    private float mOffsetY;//上一个event与当前event之间的y轴偏移量
    private PointF mLastMove = new PointF();//记录每一个event的xy轴坐标
    private float mResistance = 1.7f;//阻尼系数

    /**
     * 手指状态
     */
    private boolean mIsUnderTouch = false;//是否在触摸状态

    /**
     * 记录手指滑动的距离，相对于POS_START起始位置
     */
    public final static int POS_START = 0;//起始位置常量标识
    private int mPressedPos = 0;//手指down下的位置，和POS_START不一定是同一个位置
    private int mCurrentPos = 0;//手指当前的位置(从POS_START竖向滑动的距离)
    private int mLastPos = 0;//手指上一次的位置


    //////////////
    //位置计算
    /////////////

    /**
     * 设置为非触摸状态
     */
    public void onRelease() {
        mIsUnderTouch = false;
    }

    /**
     * 是否处于触摸状态
     */
    public boolean isUnderTouch() {
        return mIsUnderTouch;
    }

    /**
     * 手指down状态，记录xy轴坐标到mPtLastMove。并记录手指down下的位置等于当前位置
     */
    public void onPressDown(float x, float y) {
        mIsUnderTouch = true;
        mPressedPos = mCurrentPos;
        mLastMove.set(x, y);
    }

    /**
     * 手指move状态，计算出与上一个event之间xy轴上移动的距离。
     * y轴偏移量是经过除于阻尼系数，x轴偏移量保持不变。
     */
    public final void onMove(float x, float y) {
        float offsetX = x - mLastMove.x;
        float offsetY = (y - mLastMove.y);
        processOnMove(x, y, offsetX, offsetY);
        mLastMove.set(x, y);
    }

    /**
     * 设置xy轴偏移量。
     * y轴偏移量需要除于阻尼系数，x轴不做处理（仅仅用到offsetX, offsetY两个参数）
     */
    protected void processOnMove(float currentX, float currentY, float offsetX, float offsetY) {
        setOffset(offsetX, offsetY / mResistance);
    }

    /**
     * 设置xy轴的偏移量
     */
    protected void setOffset(float x, float y) {
        mOffsetX = x;
        mOffsetY = y;
    }

    /**
     * event与event之间的x轴偏移量
     */
    public float getOffsetX() {
        return mOffsetX;
    }

    /**
     * event与event之间的y轴偏移量
     */
    public float getOffsetY() {
        return mOffsetY;
    }


    /**
     * 获取上一次的位置mLastPos
     */
    public int getLastPosY() {
        return mLastPos;
    }


    /**
     * 获取当前位置mCurrentPos
     */
    public int getCurrentPosY() {
        return mCurrentPos;
    }

    /**
     * 设置当前位置，并回调protected void onUpdatePos(int current, int last)方法
     */
    public final void setCurrentPos(int current) {
        mLastPos = mCurrentPos;
        mCurrentPos = current;
        onUpdatePos(current, mLastPos);
    }

    /**
     * 位置更新后回调该方法
     */
    protected void onUpdatePos(int current, int last) {

    }

    /**
     * UI刷新完成，记录刷新完成的位置=当前位置
     */
    public void onUIRefreshComplete() {
        mRefreshCompleteY = mCurrentPos;
    }

    //////////////////////////////////////////////////////
    //临界点判断，根据位置变量计算当前手指位置到达的一些临界点
    //////////////////////////////////////////////////////

    /**
     * 赋值另外一个指示器的状态给自己，
     * 赋值mCurrentPos、mLastPos、mHeaderHeight
     */
    public void convertFrom(PtrIndicator ptrSlider) {
        mCurrentPos = ptrSlider.mCurrentPos;
        mLastPos = ptrSlider.mLastPos;
        mHeaderHeight = ptrSlider.mHeaderHeight;
    }

    /**
     * 当前位置 超过 刷新完成的位置
     */
    public boolean goDownCrossFinishPosition() {
        return mCurrentPos >= mRefreshCompleteY;
    }

    /**
     * 当前位置 在 起始位置
     */
    public boolean isInStartPosition() {
        return mCurrentPos == POS_START;
    }

    /**
     * 当前位置 已经离开 手指down下的位置
     */
    public boolean hasMovedAfterPressedDown() {
        return mCurrentPos != mPressedPos;
    }

    /**
     * 当前位置 已离开 起始位置
     */
    public boolean hasLeftStartPosition() {
        return mCurrentPos > POS_START;
    }

    /**
     * 当前位置 刚离开 起始位置
     */
    public boolean hasJustLeftStartPosition() {
        return mLastPos == POS_START && hasLeftStartPosition();
    }

    /**
     * 当前位置 刚抵达到 起始位置
     */
    public boolean hasJustBackToStartPosition() {
        return mLastPos != POS_START && isInStartPosition();
    }


    /**
     * 当前位置 超出了 触发刷新阀值
     */
    public boolean isOverOffsetToRefresh() {
        return mCurrentPos >= getOffsetToRefresh();
    }

    /**
     * 当前位置 刚抵达到 刷新触发阀值
     */
    public boolean crossRefreshLineFromTopToBottom() {
        return mLastPos < getOffsetToRefresh() && isOverOffsetToRefresh();
    }


    /**
     * 当前位置 刚抵达到 head的高度
     */
    public boolean hasJustReachedHeaderHeightFromTopToBottom() {
        return mLastPos < mHeaderHeight && mCurrentPos >= mHeaderHeight;
    }

    /**
     * 当前位置 超出 刷新时head保持的高度
     */
    public boolean isOverOffsetToKeepHeaderWhileLoading() {
        return mCurrentPos > getOffsetToKeepHeaderWhileLoading();
    }

    /**
     * 当前位置 等于 指定的to位置
     */
    public boolean isAlreadyHere(int to) {
        return mCurrentPos == to;
    }

    /**
     * 指定的to位置 小于 起始位置
     */
    public boolean willOverTop(int to) {
        return to < POS_START;
    }

    //////////////////////////
    //变量设置与获取
    /////////////////////////

    /**
     * 获取阻尼系数
     */
    public float getResistance() {
        return mResistance;
    }

    /**
     * 设置阻尼系数
     */
    public void setResistance(float resistance) {
        mResistance = resistance;
    }


    /**
     * 更新触发刷新阀值
     */
    protected void updateOffsetToRefresh() {
        mOffsetToRefresh = (int) (mHeaderHeight * mRatioOfHeaderHeightToRefresh);
    }

    /**
     * 更新head高度与触发刷新阀值的比例
     */
    protected void updateRatioOfHeaderHeightToRefresh() {
        mRatioOfHeaderHeightToRefresh = mHeaderHeight * 1f / mOffsetToRefresh;
    }

    /**
     * 设置head的高度。并更新触发刷新阀值
     */
    public void setHeaderHeight(int height) {
        mHeaderHeight = height;
        updateOffsetToRefresh();
    }

    /**
     * 获取head的高度
     */
    public int getHeaderHeight() {
        return mHeaderHeight;
    }

    /**
     * 设置触发刷新阀值。并更新head高度与触发刷新阀值的比例
     */
    public void setOffsetToRefresh(int offset) {
        mOffsetToRefresh = offset;
        updateRatioOfHeaderHeightToRefresh();
    }

    /**
     * 获取触发刷新阀值，即下拉多少距离触发刷新
     */
    public int getOffsetToRefresh() {
        return mOffsetToRefresh;
    }

    /**
     * 设置head高度与触发刷新阀值的比例。并更新触发刷新阀值
     */
    public void setRatioOfHeaderHeightToRefresh(float ratio) {
        mRatioOfHeaderHeightToRefresh = ratio;
        updateOffsetToRefresh();
    }

    /**
     * 获取head高度与触发刷新阀值的比例
     */
    public float getRatioOfHeaderToHeightRefresh() {
        return mRatioOfHeaderHeightToRefresh;
    }

    /**
     * 设置刷新时head保持的高度
     */
    public void setOffsetToKeepHeaderWhileLoading(int offset) {
        mOffsetToKeepHeaderWhileLoading = offset;
    }

    /**
     * 获取刷新时head保持的高度。
     * 默认是head的高度，可通过setOffsetToKeepHeaderWhileLoading(int offset)设其他值
     */
    public int getOffsetToKeepHeaderWhileLoading() {
        return mOffsetToKeepHeaderWhileLoading >= 0 ? mOffsetToKeepHeaderWhileLoading : mHeaderHeight;
    }

    /**
     * 获取上一次位置相对于head高度的百分比
     */
    public float getLastPercent() {
        final float oldPercent = mHeaderHeight == 0 ? 0 : mLastPos * 1f / mHeaderHeight;
        return oldPercent;
    }

    /**
     * 获取当前位置相对于head高度的百分比
     */
    public float getCurrentPercent() {
        final float currentPercent = mHeaderHeight == 0 ? 0 : mCurrentPos * 1f / mHeaderHeight;
        return currentPercent;
    }


}
