package in.srain.cube.views.ptr;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;
import android.widget.TextView;

import in.srain.cube.views.ptr.indicator.PtrIndicator;
import in.srain.cube.views.ptr.util.PtrCLog;

/**
 * 下拉刷新功能：<br>
 * 1、head和content的布局 <br>
 * 2、action event的分发、过滤、补发cancel和down给子view <br>
 * 3、head的回滚通过ScrollChecker来实现，自动下拉也是 <br>
 * 4、action event 和 ScrollChecker开始|打断|结束 都是调用同样函数处理--->movePos()和 onRelease() <br>
 * 5、定义好状态，并规定状态转换的规则，即状态对应的动作 <br>
 * 6、精确的位置计算器PtrIndicator <br>
 */
public class PtrFrameLayout extends ViewGroup {

    /**
     * 一、updatePos(int change)位置更新事件(手指拖动、自动下拉)。
     * 二、updatePos(int change)位置释放事件(手指up、ScrollChecker滚动被打断或结束)
     * <p>
     * 状态常量：初始化状态、下拉状态、加载中状态、加载完成状态
     * --------------------------
     * <p>
     * 1、转为初始状态的情况：<br>
     * （1）“位置更新事件”和“尝试通知转为加载完成状态”的时候：(上一个状态是：加载完成状态||下拉状态）&& 当前位置在初始位置<br>
     * 为何要在“尝试通知转为加载完成状态”的情况下，尝试转为初始化状态？<br>
     * 因为用户在不等加载完成的情况下，直接上滑让head已经回到初始位置<br>
     * --------------------------
     * <p>
     * 2、转为下拉状态的情况：<br>
     * （1）“位置更新事件”的时候：（上一个状态是初始化状态 && 刚离开初始位置 ）||（上一个状态是加载完成状态 && 超过加载完成位置 && 允许再次下拉刷新）<br>
     * （2）调用自动下拉刷新的时候<br>
     * ---------------------------
     * <p>
     * 3、转为加载中状态的情况：<br>
     * （1）“位置更新事件”和“位置释放事件”的时候：【上一个状态为下拉状态】&&【（自动下拉刷新&&当前位置“超过”刷新时head保持的高度）|| 当前位置“超过”触发刷新阀值】<br/>
     * （2）调用自动下拉刷新并且是立即刷新的时候<br>
     * --------------------------
     * <p>
     * 4、转为加载完成的状态：
     * （1）手动调用refreshComplete()加载完成函数。注意：加载完成之后接下来的状态都是加载完成的状态，除非在2(1)的情况下把加载完成状态又置为下拉状态。
     */
    public final static byte PTR_STATUS_INIT = 1;//初始化状态
    public final static byte PTR_STATUS_PREPARE = 2;//下拉状态 
    public final static byte PTR_STATUS_LOADING = 3;//加载中状态
    public final static byte PTR_STATUS_COMPLETE = 4;//加载完成状态
    private byte mStatus = PTR_STATUS_INIT;

    /**
     * 调试变量
     */
    private static final boolean DEBUG_LAYOUT = true;
    public static boolean DEBUG = true;
    private static int ID = 1;
    protected final String LOG_TAG = "ptr-frame-" + ++ID;

    /**
     * 自动下拉刷新的状态
     */
    private int mFlag = 0x00;//位运算符，属性表包含关系
    private final static byte FLAG_AUTO_REFRESH_AT_ONCE = 0x01;//立即下拉刷新
    private final static byte FLAG_AUTO_REFRESH_BUT_LATER = 0x01 << 1;//content滚动到顶部，再进行自动下拉刷新
    private final static byte FLAG_ENABLE_NEXT_PTR_AT_ONCE = 0x01 << 2;//加载完成后状态 && 未回到初始化状态时：再次下拉，当超过触发刷新阀值时是否再次调用tryToPerformRefreshBegin()即刷新事件
    private final static byte MASK_AUTO_REFRESH = 0x03;//提取是自动下拉刷新的mask

    private final static byte FLAG_PIN_CONTENT = 0x01 << 3;//下拉是否保持Content位置不变


    /**
     * 包含的内部控件
     */
    protected View mContent;
    private View mHeaderView;
    // optional config for define header and content in xml file
    private int mHeaderId = 0;
    private int mContainerId = 0;

    /**
     * 配置
     */
    private int mDurationToClose = 200;//回弹到触发刷新阀值的时间
    private int mDurationToCloseHeader = 1000;//head回弹到0的时间
    private boolean mKeepHeaderWhenRefresh = true;//刷新时是否保持头部，false时head会直接回到0
    private boolean mPullToRefresh = false;//到达触发刷新阀值，是自动刷新还是释放才刷新，true自动刷新，false释放刷新

    /**
     * head处理器，要求head实现PtrHandler接口，由PtrUIHandlerHolder组成责任链
     */
    private PtrUIHandlerHolder mPtrUIHandlerHolder = PtrUIHandlerHolder.create();
    private PtrHandler mPtrHandler;

    // working parameters
    private ScrollChecker mScrollChecker;//实现自动下拉刷新
    private int mPagingTouchSlop;//认为是滑动的临界值
    private int mHeaderHeight;//head的高度，包含了head设置的marging


    private boolean mDisableWhenHorizontalMove = false;//横向滑动时是否禁用下拉刷新
    private boolean mPreventForHorizontal = false;//辅助变量，tur表示此次动作是横向滑动，false表示不是横向

    private MotionEvent mLastMoveEvent;//最近一次的moven event事件
    private PtrUIHandlerHook mRefreshCompleteHook;

    private int mLoadingMinTime = 500;//最小刷新时间
    private long mLoadingStartTime = 0;
    private PtrIndicator mPtrIndicator;//计算器
    private boolean mHasSendCancelEvent = false;//是否已发送过cancel事件到子view

    private Runnable mPerformRefreshCompleteDelay = new Runnable() {
        @Override
        public void run() {
            performRefreshComplete();
        }
    };

    public PtrFrameLayout(Context context) {
        this(context, null);
    }

    public PtrFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PtrFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mPtrIndicator = new PtrIndicator();

        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.PtrFrameLayout, 0, 0);
        if (arr != null) {

            mHeaderId = arr.getResourceId(R.styleable.PtrFrameLayout_ptr_header, mHeaderId);
            mContainerId = arr.getResourceId(R.styleable.PtrFrameLayout_ptr_content, mContainerId);

            mPtrIndicator.setResistance(arr.getFloat(R.styleable.PtrFrameLayout_ptr_resistance, mPtrIndicator.getResistance()));

            mDurationToClose = arr.getInt(R.styleable.PtrFrameLayout_ptr_duration_to_close, mDurationToClose);
            mDurationToCloseHeader = arr.getInt(R.styleable.PtrFrameLayout_ptr_duration_to_close_header, mDurationToCloseHeader);

            float ratio = mPtrIndicator.getRatioOfHeaderToHeightRefresh();
            ratio = arr.getFloat(R.styleable.PtrFrameLayout_ptr_ratio_of_header_height_to_refresh, ratio);
            mPtrIndicator.setRatioOfHeaderHeightToRefresh(ratio);

            mKeepHeaderWhenRefresh = arr.getBoolean(R.styleable.PtrFrameLayout_ptr_keep_header_when_refresh, mKeepHeaderWhenRefresh);

            mPullToRefresh = arr.getBoolean(R.styleable.PtrFrameLayout_ptr_pull_to_fresh, mPullToRefresh);
            arr.recycle();
        }

        mScrollChecker = new ScrollChecker();

        final ViewConfiguration conf = ViewConfiguration.get(getContext());
        mPagingTouchSlop = conf.getScaledTouchSlop() * 2;
    }

    /**
     * 配置head和content
     */
    @Override
    protected void onFinishInflate() {

        final int childCount = getChildCount();
        if (childCount > 2) {
            throw new IllegalStateException("PtrFrameLayout can only contains 2 children");
        } else if (childCount == 2) {
            if (mHeaderId != 0 && mHeaderView == null) {
                mHeaderView = findViewById(mHeaderId);
            }
            if (mContainerId != 0 && mContent == null) {
                mContent = findViewById(mContainerId);
            }

            // not specify header or content
            if (mContent == null || mHeaderView == null) {

                View child1 = getChildAt(0);
                View child2 = getChildAt(1);
                if (child1 instanceof PtrUIHandler) {
                    mHeaderView = child1;
                    mContent = child2;
                } else if (child2 instanceof PtrUIHandler) {
                    mHeaderView = child2;
                    mContent = child1;
                } else {
                    // both are not specified
                    if (mContent == null && mHeaderView == null) {
                        mHeaderView = child1;
                        mContent = child2;
                    }
                    // only one is specified
                    else {
                        if (mHeaderView == null) {
                            mHeaderView = mContent == child1 ? child2 : child1;
                        } else {
                            mContent = mHeaderView == child1 ? child2 : child1;
                        }
                    }
                }
            }
        } else if (childCount == 1) {
            mContent = getChildAt(0);
        } else {
            TextView errorView = new TextView(getContext());
            errorView.setClickable(true);
            errorView.setTextColor(0xffff6600);
            errorView.setGravity(Gravity.CENTER);
            errorView.setTextSize(20);
            errorView.setText("The content view in PtrFrameLayout is empty. Do you forget to specify its id in xml layout file?");
            mContent = errorView;
            addView(mContent);
        }

        if (mHeaderView != null) {
            mHeaderView.bringToFront();//将head带至最顶层
        }
        super.onFinishInflate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //直接销毁可能照成的问题：
        // （1）将PtrFrameLayout remove掉又add回来，可能造成状态丢失
        // （2）在有对contentview进行保存的fragment中，当fragment被remove掉再add回来，可能造成PtrFrameLayout状态丢失
        if (mScrollChecker != null) {
            mScrollChecker.destroy();//销毁自动刷新
        }
        if (mPerformRefreshCompleteDelay != null) {
            //刷新时间<最小刷新时间会发这样一个runnable
            removeCallbacks(mPerformRefreshCompleteDelay);//销毁未完成消息线程异步消息
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (isDebug()) {
            PtrCLog.d(LOG_TAG, "onMeasure frame: width: %s, height: %s, padding: %s %s %s %s",
                    getMeasuredHeight(), getMeasuredWidth(), getPaddingLeft(), getPaddingRight(), getPaddingTop(), getPaddingBottom());
        }

        if (mHeaderView != null) {

            //测量headview的大小，给予子view的空间大小，去掉了自身设置的padding和子view设置的marging
            measureChildWithMargins(mHeaderView, widthMeasureSpec, 0, heightMeasureSpec, 0);
            MarginLayoutParams lp = (MarginLayoutParams) mHeaderView.getLayoutParams();
            mHeaderHeight = mHeaderView.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            mPtrIndicator.setHeaderHeight(mHeaderHeight);
        }

        if (mContent != null) {
            measureContentView(mContent, widthMeasureSpec, heightMeasureSpec);
            if (isDebug()) {
                ViewGroup.MarginLayoutParams lp = (MarginLayoutParams) mContent.getLayoutParams();
                PtrCLog.d(LOG_TAG, "onMeasure content, width: %s, height: %s, margin: %s %s %s %s", getMeasuredWidth(), getMeasuredHeight(), lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin);
                PtrCLog.d(LOG_TAG, "onMeasure, currentPos: %s, lastPos: %s, top: %s", mPtrIndicator.getCurrentPosY(), mPtrIndicator.getLastPosY(), mContent.getTop());
            }
        }
    }

    /**
     * measureChildWithMargins()方法没区别，也是去掉了自身设置的padding和子view设置的marging
     */
    private void measureContentView(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin, lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                getPaddingTop() + getPaddingBottom() + lp.topMargin, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean flag, int i, int j, int k, int l) {
        layoutChildren();
    }

    /**
     * 布局子view的位置
     */
    private void layoutChildren() {

        int offset = mPtrIndicator.getCurrentPosY();

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        if (mHeaderView != null) {
            MarginLayoutParams lp = (MarginLayoutParams) mHeaderView.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            // enhance readability(header is layout above screen when first init)
            final int top = -(mHeaderHeight - paddingTop - lp.topMargin - offset);
            final int right = left + mHeaderView.getMeasuredWidth();
            final int bottom = top + mHeaderView.getMeasuredHeight();
            mHeaderView.layout(left, top, right, bottom);
            if (isDebug()) {
                PtrCLog.d(LOG_TAG, "onLayout header: %s %s %s %s", left, top, right, bottom);
            }
        }
        if (mContent != null) {
            if (isPinContent()) {
                offset = 0;
            }
            MarginLayoutParams lp = (MarginLayoutParams) mContent.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            final int top = paddingTop + lp.topMargin + offset;
            final int right = left + mContent.getMeasuredWidth();
            final int bottom = top + mContent.getMeasuredHeight();
            if (isDebug()) {
                PtrCLog.d(LOG_TAG, "onLayout content: %s %s %s %s", left, top, right, bottom);
            }
            mContent.layout(left, top, right, bottom);
        }
    }


    /**
     * 通过是否调用super.dispatchTouchEvent()来决定是不是把时间发给childview
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {

        if (!isEnabled() || mContent == null || mHeaderView == null) {
            return dispatchTouchEventSupper(e);
        }

        int action = e.getAction();
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

                mPtrIndicator.onRelease();

                if (mPtrIndicator.hasLeftStartPosition()) {//是下拉刷新事件
                    if (DEBUG) {
                        PtrCLog.d(LOG_TAG, "call onRelease when user release");
                    }
                    onRelease(true);

                    if (mPtrIndicator.hasMovedAfterPressedDown()) {//是下拉刷新事件
                        sendCancelEvent();
                        return true;
                    }
                    return dispatchTouchEventSupper(e);
                } else {
                    //不属于下拉刷新的事件
                    return dispatchTouchEventSupper(e);
                }

            case MotionEvent.ACTION_DOWN:

                //记录down事件，并下发给子view。down是事件线的针，dwon事件时该函数的返回值很重要。
                mHasSendCancelEvent = false;
                mPreventForHorizontal = false;
                mPtrIndicator.onPressDown(e.getX(), e.getY());
                mScrollChecker.abortIfWorking();//强制停止ScrollChecker

                // The cancel event will be sent once the position is moved.
                // So let the event pass to children.
                // fix #93, #102
                dispatchTouchEventSupper(e);//必须下发down事件，子view不接收down事件就没办法把子view串进事件线
                return true;//不管super.dispatch()的返回值，直接返回true，表明自己是一定在事件线里的

            case MotionEvent.ACTION_MOVE:

                mLastMoveEvent = e;
                mPtrIndicator.onMove(e.getX(), e.getY());
                float offsetX = mPtrIndicator.getOffsetX();
                float offsetY = mPtrIndicator.getOffsetY();

                //过滤掉横向滑动的move
                if (mDisableWhenHorizontalMove &&
                        !mPreventForHorizontal &&
                        (Math.abs(offsetX) > mPagingTouchSlop && Math.abs(offsetX) > Math.abs(offsetY))
                ) {

                    //起点位置的判断，mPtrIndicator的mCurrentPos只有进入到时movePos()方法后的时候才会改变。
                    if (mPtrIndicator.isInStartPosition()) {
                        mPreventForHorizontal = true;//辅助变量，tur表示此次动作是横向滑动，false表示不是横向
                    }

                }
                if (mPreventForHorizontal) {//横向滑动不处理，直接下发
                    return dispatchTouchEventSupper(e);
                }

                boolean moveDown = offsetY > 0;//下拉，是相对于上一个event的位置来说的
                boolean moveUp = !moveDown;//上滑
                boolean canMoveUp = mPtrIndicator.hasLeftStartPosition();//能上滑，是相对于起始位置来说的

                if (DEBUG) {
                    boolean canMoveDown = mPtrHandler != null && mPtrHandler.checkCanDoRefresh(this, mContent, mHeaderView);
                    PtrCLog.v(LOG_TAG, "ACTION_MOVE: offsetY:%s, currentPos: %s, moveUp: %s, canMoveUp: %s, moveDown: %s: canMoveDown: %s",
                            offsetY, mPtrIndicator.getCurrentPosY(), moveUp, canMoveUp, moveDown, canMoveDown);
                }

                //过滤掉content没滚动到顶部的move
                if (moveDown && mPtrHandler != null && !mPtrHandler.checkCanDoRefresh(this, mContent, mHeaderView)) {
                    //下拉，mPtrHandler不为null，mPtrHandler的check返回false(content是否已经滑到顶部)，将事件下发给子view
                    return dispatchTouchEventSupper(e);
                }

                //过滤掉不是下拉刷新的move
                if ((moveUp && canMoveUp) || moveDown) {
                    //上滑&&能够上滑：对应head下拉出现后，上滑回去。
                    // 上滑&&不能够上滑，就把事件下发：对应head上滑回去之后继续上滑时，要把事件还给子view。
                    movePos(offsetY);

                    return true;//后续事件中的返回值已经无所谓了，返回true或false都可以，主要是有没有调用super.dispatch()把事件下发下去。

                } else {
                    return dispatchTouchEventSupper(e);
                }
        }
        return dispatchTouchEventSupper(e);
    }


    public boolean dispatchTouchEventSupper(MotionEvent e) {
        return super.dispatchTouchEvent(e);
    }

    /**
     * 位置移动函数：在“move event”和“ScrollChecker开始”的时候调用，对不符合的移动做过滤和对不合适的距离做限制。
     *
     * @param deltaY event与event移动的距离
     */
    private void movePos(float deltaY) {
        // has reached the top
        //又做了一次过滤，head已经到达顶部了，还继续上滑的需要过滤掉。
        //问题：当快接近顶部的时候(moveUp&&canMoveUp仍为true，即isInStartPosition=false)，还继续上滑。在dispatchTouchEvent()是没法过滤掉的，因为mPtrIndicator的mCurrentPos是在接下来的代码里才赋值的
        if ((deltaY < 0 && mPtrIndicator.isInStartPosition())) {
            if (DEBUG) {
                PtrCLog.e(LOG_TAG, String.format("has reached the top"));
            }
            return;
        }

        int to = mPtrIndicator.getCurrentPosY() + (int) deltaY;//head停留的终点位置

        //过滤head停留的目标位置超出了起始位置， over top
        if (mPtrIndicator.willOverTop(to)) {
            if (DEBUG) {
                PtrCLog.e(LOG_TAG, String.format("over top"));
            }
            //上滑时，to的位置如果超出起始位置，就直接设置为起始位置。
            // 问题出现在：当快接近顶部的时候(moveUp&&canMoveUp仍为true)，快速上滑的到deltaY的值比较大，超出了起始位置。
            to = PtrIndicator.POS_START;
        }

        //更改mPtrIndicator的mCurrentPos的值
        mPtrIndicator.setCurrentPos(to);

        int change = to - mPtrIndicator.getLastPosY();//一般情况下change和deltaY相等，但是当to位置超出顶部的话，change就会被压制在0到当前位置的距离
        if (DEBUG) {
            PtrCLog.v(LOG_TAG, String.format("deltaY:%s, change:%s", deltaY, change));
        }
        updatePos(change);
    }

    /**
     * 位置移动函数：在“move event”和“ScrollChecker开始”的时候调用。
     *
     * @param change
     */
    private void updatePos(int change) {
        if (change == 0) {
            return;
        }

        boolean isUnderTouch = mPtrIndicator.isUnderTouch();

        //准备刷新前：属于下拉刷新事件。需要发送一个cancel事件给之前接收到down和move的子view
        if (isUnderTouch && !mHasSendCancelEvent && mPtrIndicator.hasMovedAfterPressedDown()) {
            mHasSendCancelEvent = true;//在down的时候重新设置false，控制值发送一次
            //由于down 0-n个move 有下发到子view的，所以当拦截的move后，需要发一个cancel事件给子view，达到取消子view接收到down或后续一点move事件效果，比如背景变色什么的
            sendCancelEvent();
        }

        //（刚离开初始位置 && 上一个状态是初始化状态）或者 （超过刷新时位置 && 上一个状态是加载完成状态 && 允许再次下拉刷新）
        if ((mPtrIndicator.hasJustLeftStartPosition() && mStatus == PTR_STATUS_INIT) ||
                (mPtrIndicator.goDownCrossFinishPosition() && mStatus == PTR_STATUS_COMPLETE && isEnabledNextPtrAtOnce())
        ) {
            mStatus = PTR_STATUS_PREPARE;
            mPtrUIHandlerHolder.onUIRefreshPrepare(this);
            if (DEBUG) {
                PtrCLog.i(LOG_TAG, "PtrUIHandler: onUIRefreshPrepare, mFlag %s", mFlag);
            }
        }

        // 刚回到初始位置：
        if (mPtrIndicator.hasJustBackToStartPosition()) {
            tryToNotifyReset();//尝试转为初始化状态

            if (isUnderTouch) {
                //在触摸状态下，又刚回到起始位置时。又需要补发down事件给子view，实现对子view事件的恢复。
                sendDownEvent();
            }
        }

        if (mStatus == PTR_STATUS_PREPARE) {
            // 触摸状态下 && 不是自动下拉刷新 && 下拉到阀值自动刷新 && 当前位置刚抵达触发刷新阀值
            if (isUnderTouch && !isAutoRefresh() && mPullToRefresh && mPtrIndicator.crossRefreshLineFromTopToBottom()) {
                tryToPerformRefreshBegin();
            }

            // 延迟自动下拉刷新 && 当前位置刚好抵达head高度
            if (isAutoRefreshButLater() && mPtrIndicator.hasJustReachedHeaderHeightFromTopToBottom()) {
                tryToPerformRefreshBegin();
            }
        }

        if (DEBUG) {
            PtrCLog.v(LOG_TAG, "updatePos: change: %s, current: %s last: %s, top: %s, headerHeight: %s",
                    change, mPtrIndicator.getCurrentPosY(), mPtrIndicator.getLastPosY(), mContent.getTop(), mHeaderHeight);
        }

        //移动head和content的位置
        mHeaderView.offsetTopAndBottom(change);
        if (!isPinContent()) {
            mContent.offsetTopAndBottom(change);
        }

        invalidate();//发起一次重绘？

        if (mPtrUIHandlerHolder.hasHandler()) {
            mPtrUIHandlerHolder.onUIPositionChange(this, isUnderTouch, mStatus, mPtrIndicator);
        }
        onPositionChange(isUnderTouch, mStatus, mPtrIndicator);
    }

    /**
     * 位置发生改变的回调
     */
    protected void onPositionChange(boolean isInTouching, byte status, PtrIndicator mPtrIndicator) {
    }

    /**
     * （event UP|Cancel） 和 （“ScrollerChecker被打断或自然结束”&&“是自动刷新”&&“不在初始位置”）两种情况下调用<p>
     * 位置释放的情况下，处理当前状态对应的动作
     *
     * @param stayForLoading 是否需要触发-->回滚至刷新时head保持的高度的额外控制变量。主要用来区分是“event up|cancel”？还是ScrollChecker停止？<br>
     *                       只有(event up|cancel)才需要回滚至刷新时head保持的高度，ScrollChecker停止是不需要的! <br>
     *                       因为ScrollChecker自动下拉刷新最多也就是下拉到刷新时head保持的高度。
     */
    private void onRelease(boolean stayForLoading) {

        tryToPerformRefreshBegin();

        if (mStatus == PTR_STATUS_LOADING) {

            //加载中的状态,ScrollChecker回滚有两种情况：1、回滚至初始位置。2、回滚至刷新时head保持的高度
            if (mKeepHeaderWhenRefresh) {
                tryScrollBackToKeepHeaderWhileLoading(stayForLoading);
            } else {
                tryScrollBackToTopWhileLoading();
            }

        } else if (mStatus == PTR_STATUS_COMPLETE) {
            //加载完成的状态，通知UI动画加载完成，通知ScrollChecker回滚至初始位置
            notifyUIRefreshComplete(false);
        } else {
            //通知ScrollChecker回滚至初始位置
            tryScrollBackToTopAbortRefresh();
        }

    }

    /**
     * 创建一个最近一次move event位置的cancel事件给content
     */
    private void sendCancelEvent() {
        if (DEBUG) {
            PtrCLog.d(LOG_TAG, "send cancel event");
        }
        // The ScrollChecker will update position and lead to send cancel event when mLastMoveEvent is null.
        // fix #104, #80, #92
        if (mLastMoveEvent == null) {
            return;
        }
        MotionEvent last = mLastMoveEvent;
        MotionEvent e = MotionEvent.obtain(last.getDownTime(), last.getEventTime() + ViewConfiguration.getLongPressTimeout(), MotionEvent.ACTION_CANCEL, last.getX(), last.getY(), last.getMetaState());
        dispatchTouchEventSupper(e);
    }

    /**
     * 创建一个最近一次move event位置的down事件给content
     */
    private void sendDownEvent() {
        if (DEBUG) {
            PtrCLog.d(LOG_TAG, "send down event");
        }
        final MotionEvent last = mLastMoveEvent;
        MotionEvent e = MotionEvent.obtain(last.getDownTime(), last.getEventTime(), MotionEvent.ACTION_DOWN, last.getX(), last.getY(), last.getMetaState());
        dispatchTouchEventSupper(e);
    }

    ///////////////////
    //ScrollerChecker回滚，结束后的回调
    //////////////////

    /**
     * 强制停止自动回滚时调用，自动刷新&&不在初始位置回调onRelease(true);
     */
    protected void onPtrScrollAbort() {
        if (mPtrIndicator.hasLeftStartPosition() && isAutoRefresh()) {
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "call onRelease after scroll abort");
            }
            onRelease(false);
        }
    }

    /**
     * 自动回滚结束时，自动刷新&&不在初始位置回调onRelease(true);
     */
    protected void onPtrScrollFinish() {
        if (mPtrIndicator.hasLeftStartPosition() && isAutoRefresh()) {
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "call onRelease after scroll finish");
            }
            onRelease(false);
        }
    }

    ////////////////////
    //转为加载完成状态
    ///////////////////

    /**
     * 为刷新完成配置钩子任务
     */
    public void setRefreshCompleteHook(PtrUIHandlerHook hook) {
        mRefreshCompleteHook = hook;

        hook.setResumeAction(new Runnable() {
            @Override
            public void run() {
                if (DEBUG) {
                    PtrCLog.d(LOG_TAG, "mRefreshCompleteHook resume.");
                }
                notifyUIRefreshComplete(true);
            }
        });
    }

    /**
     * 刷新完成。<br>
     * 1、刷新时长 > 设置最小刷新时长，会立即调用performRefreshComplete()
     * 2、刷新时长 < 设置最小刷新时,会延迟调用performRefreshComplete()
     */
    final public void refreshComplete() {

        if (DEBUG) {
            PtrCLog.i(LOG_TAG, "refreshComplete");
        }
        if (mRefreshCompleteHook != null) {
            mRefreshCompleteHook.reset();
        }

        int delay = (int) (mLoadingMinTime - (System.currentTimeMillis() - mLoadingStartTime));

        if (delay <= 0) {
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "performRefreshComplete at once");
            }
            performRefreshComplete();

        } else {
            postDelayed(mPerformRefreshCompleteDelay, delay);
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "performRefreshComplete after delay: %s", delay);
            }
        }

    }

    /**
     * 转为加载完成状态
     * <p>
     * 1、如果不是自动刷新，会调用notifyUIRefreshComplete()
     * 2、如果是自动下拉刷新，则不需要调用notifyUIRefreshComplete()，等待mScrollChecker自己执行
     */
    private void performRefreshComplete() {

        mStatus = PTR_STATUS_COMPLETE;

        //如果是自动下拉刷新，不需要去通知通知ScrollerChecker要回到初始位置。
        // 只需要等mScrollChecker自己执行就可以了，ScrollChecker结束后会去调用onPtrScrollFinish()-->onRelease（false）-->notifyUIRefreshComplete（false）
        if (mScrollChecker.mIsRunning && isAutoRefresh()) {
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "performRefreshComplete do nothing, scrolling: %s, auto refresh: %s", mScrollChecker.mIsRunning, mFlag);
            }
            return;
        }

        notifyUIRefreshComplete(false);
    }


    /**
     * 转为加载完成状态后执行的操作。通知UI动画加载完成,通知ScrollerChecker要回到初始位置
     *
     * @param ignoreHook 是否忽略钩子任务
     */
    private void notifyUIRefreshComplete(boolean ignoreHook) {

        if (mPtrIndicator.hasLeftStartPosition() && !ignoreHook && mRefreshCompleteHook != null) {
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "notifyUIRefreshComplete mRefreshCompleteHook run.");
            }
            mRefreshCompleteHook.takeOver();
            return;
        }

        if (mPtrUIHandlerHolder.hasHandler()) {
            if (DEBUG) {
                PtrCLog.i(LOG_TAG, "PtrUIHandler: onUIRefreshComplete");
            }
            mPtrUIHandlerHolder.onUIRefreshComplete(this);
        }

        mPtrIndicator.onUIRefreshComplete();//记录刷新完成是的位置为当前位置
        tryScrollBackToTopAfterComplete();
        tryToNotifyReset();//在被通知加载完成时，需要尝试通知回到初始状态。因为用户在不等加载完成的情况下，head已经回到初始位置
    }

    //////////////////////////
    //转为加载中状态
    /////////////////////////

    /**
     * 尝试转为加载中的状态 <br/>
     * 1、“位置更新事件”和“位置释放事件”中调用：
     * (1) 当前状态必须是下拉状态
     * (2)（自动下拉刷 && 当前位置“超过”刷新时head保持的高度） || 当前位置“超过”触发刷新阀值<br/>
     * 所以自动下拉刷新的情况，只要达到刷新时head保持的高度，就会触发加载中状态
     */
    private boolean tryToPerformRefreshBegin() {

        if (mStatus != PTR_STATUS_PREPARE) {
            return false;
        }

        if ((mPtrIndicator.isOverOffsetToKeepHeaderWhileLoading() && isAutoRefresh()) || mPtrIndicator.isOverOffsetToRefresh()) {
            performRefreshBegin();
        }
        return false;
    }

    /**
     * 直接转为加载中状态，通知UI动画“开始”，并通知事件回调开始执行刷新<br></>
     * 在调用自动下拉刷新且是立即刷新的时候会被调用 和 tryToPerformRefreshBegin()中被调用
     */
    private void performRefreshBegin() {
        mStatus = PTR_STATUS_LOADING;
        mLoadingStartTime = System.currentTimeMillis();
        if (mPtrUIHandlerHolder.hasHandler()) {
            mPtrUIHandlerHolder.onUIRefreshBegin(this);
            if (DEBUG) {
                PtrCLog.i(LOG_TAG, "PtrUIHandler: onUIRefreshBegin");
            }
        }
        if (mPtrHandler != null) {
            mPtrHandler.onRefreshBegin(this);
        }
    }


    ///////////////////////////////
    //通过ScollerChecker回滚head
    //////////////////////////////

    /**
     * 在非触摸状态下，尝试通过Scroller，将head回滚到初始位置，即head上滑到隐藏状态
     */
    private void tryScrollBackToTop() {
        if (!mPtrIndicator.isUnderTouch()) {
            mScrollChecker.tryToScrollTo(PtrIndicator.POS_START, mDurationToCloseHeader);
        }
    }

    /**
     * 尝试通过ScrollChecker，将head回滚到初始位置(前提是非触摸状态下)<p>
     * 加载中 && mKeepHeaderWhenRefresh=false 时调用
     */
    private void tryScrollBackToTopWhileLoading() {
        tryScrollBackToTop();
    }

    /**
     * 尝试通过ScrollChecker，将head回滚到初始位置(前提是非触摸状态下)<p>
     * 加载完成时调用
     */
    private void tryScrollBackToTopAfterComplete() {
        tryScrollBackToTop();
    }

    /**
     * 尝试通过ScrollChecker，将head回滚到初始位置(前提是非触摸状态下)
     * “ScrollerChecker被打断”&&“是自动刷新”&&“不在初始位置”时调用
     */
    private void tryScrollBackToTopAbortRefresh() {
        tryScrollBackToTop();
    }

    /**
     * 尝试通过ScrollChecker，将head “回滚到” 刷新时head保持的高度<p>
     * 能够回滚的条件：<br>
     * (1) 当前位置“超过” 刷新时head保持的高度 <br>
     * (2) stayForLoading 等于 true <br>
     */
    private void tryScrollBackToKeepHeaderWhileLoading(boolean stayForLoading) {
        if (mPtrIndicator.isOverOffsetToKeepHeaderWhileLoading() && stayForLoading) {
            mScrollChecker.tryToScrollTo(mPtrIndicator.getOffsetToKeepHeaderWhileLoading(), mDurationToClose);
        }
    }

    ////////////////////////////
    //触发UI动画回到初始状态
    ////////////////////////////

    /**
     * 尝试转为初始状态<br>
     * 1、在“位置更新事件”和“尝试通知转为加载完成状态的”的时候调用：(上一个状态是：加载完成状态||下拉状态）&& 当前位置在初始位置<br>
     * <p>
     * 符合条件才会通知UI动画回到初始状态，并将mStatus改为PTR_STATUS_INIT，并且清除自动下拉falg<br>
     */
    private boolean tryToNotifyReset() {
        if ((mStatus == PTR_STATUS_COMPLETE || mStatus == PTR_STATUS_PREPARE) && mPtrIndicator.isInStartPosition()) {
            if (mPtrUIHandlerHolder.hasHandler()) {
                mPtrUIHandlerHolder.onUIReset(this);
                if (DEBUG) {
                    PtrCLog.i(LOG_TAG, "PtrUIHandler: onUIReset");
                }
            }
            mStatus = PTR_STATUS_INIT;
            clearFlag();
            return true;
        }
        return false;
    }


    //////////////
    //自动下拉刷新
    /////////////

    /**
     * 自动下拉刷新
     */
    public void autoRefresh() {
        autoRefresh(true, mDurationToCloseHeader);
    }

    /**
     * 自动下拉刷新
     *
     * @param atOnce 是否立即
     */
    public void autoRefresh(boolean atOnce) {
        autoRefresh(atOnce, mDurationToCloseHeader);
    }

    public void autoRefresh(boolean atOnce, int duration) {

        if (mStatus != PTR_STATUS_INIT) {//只有是初始状态才能进入
            return;
        }
        //添加自动下拉刷新标识
        mFlag |= atOnce ? FLAG_AUTO_REFRESH_AT_ONCE : FLAG_AUTO_REFRESH_BUT_LATER;

        //必须改为下拉状态，在mScrollChecker finish结束的时候才能触发tryToPerformRefreshBegin()
        mStatus = PTR_STATUS_PREPARE;

        if (mPtrUIHandlerHolder.hasHandler()) {
            mPtrUIHandlerHolder.onUIRefreshPrepare(this);
            if (DEBUG) {
                PtrCLog.i(LOG_TAG, "PtrUIHandler: onUIRefreshPrepare, mFlag %s", mFlag);
            }
        }

        mScrollChecker.tryToScrollTo(mPtrIndicator.getOffsetToRefresh(), duration);

        if (atOnce) {//如果是立即刷新，不需要等mScrollChecker滚动到触发刷新位置，就可以直接触发开始刷新
            performRefreshBegin();
        }
    }

    //==============================


    /**
     * Detect whether is refreshing.
     *
     * @return
     */
    public boolean isRefreshing() {
        return mStatus == PTR_STATUS_LOADING;
    }


    /**
     * 是否是自动下拉刷新
     */
    public boolean isAutoRefresh() {
        return (mFlag & MASK_AUTO_REFRESH) > 0;
    }

    /**
     * 清除mFlag自动刷新
     */
    private void clearFlag() {
        // remove auto fresh flag
        mFlag = mFlag & ~MASK_AUTO_REFRESH;
    }


    private boolean isAutoRefreshButLater() {
        return (mFlag & MASK_AUTO_REFRESH) == FLAG_AUTO_REFRESH_BUT_LATER;
    }


    /**
     * 加载完成后状态 && 未回到初始化状态时：再次下拉，当超过触发刷新阀值时是否再次调用tryToPerformRefreshBegin()即刷新事件
     */
    public boolean isEnabledNextPtrAtOnce() {
        return (mFlag & FLAG_ENABLE_NEXT_PTR_AT_ONCE) > 0;
    }

    /**
     * If @param enable has been set to true. The user can perform next PTR at once.
     * 是否允许再次自动下拉率先呢
     *
     * @param enable
     */
    public void setEnabledNextPtrAtOnce(boolean enable) {
        if (enable) {
            mFlag = mFlag | FLAG_ENABLE_NEXT_PTR_AT_ONCE;
        } else {
            mFlag = mFlag & ~FLAG_ENABLE_NEXT_PTR_AT_ONCE;
        }
    }


    /**
     * 实现head从currentPos到to位置的自动滑动(实现自动下拉和自动缩回)
     * 原理：通过对Scroller的间隔性检查，实现自动
     */
    class ScrollChecker implements Runnable {

        private int mLastFlingY;//最近一次scroller在y轴上的滑动距离

        private Scroller mScroller;
        private boolean mIsRunning = false;
        private int mStart;
        private int mTo;

        public ScrollChecker() {
            mScroller = new Scroller(getContext());
        }

        public void run() {

            boolean finish = !mScroller.computeScrollOffset() || mScroller.isFinished();

            int curY = mScroller.getCurrY();
            int deltaY = curY - mLastFlingY;//上次位置与这次位置的距离

            if (DEBUG) {
                if (deltaY != 0) {
                    PtrCLog.v(LOG_TAG,
                            "scroll: %s, start: %s, to: %s, currentPos: %s, current :%s, last: %s, delta: %s",
                            finish, mStart, mTo, mPtrIndicator.getCurrentPosY(), curY, mLastFlingY, deltaY);
                }
            }

            if (!finish) {
                //Scroller滚动未结束
                mLastFlingY = curY;
                movePos(deltaY);
                post(this);//未结束要持续检查，直到结束位置
            } else {
                //Scroller滚动结束
                finish();
            }
        }

        /**
         * 重置ScrollChecker状态,停止对Scroller的检查
         */
        private void reset() {
            mIsRunning = false;
            mLastFlingY = 0;
            removeCallbacks(this);
        }

        /**
         * 自然结束，停止对Scroller的检查结束。重置了ScrollChecker状态 <p>
         * 回到onPtrScrollFinish();
         */
        private void finish() {
            if (DEBUG) {
                PtrCLog.v(LOG_TAG, "finish, currentPos:%s", mPtrIndicator.getCurrentPosY());
            }
            reset();
            onPtrScrollFinish();
        }


        /**
         * 强制停止scroller结束，并重置了ScrollChecker状态 <p>
         * 不做任何回调
         */
        private void destroy() {
            if (!mScroller.isFinished()) {
                mScroller.forceFinished(true);
            }
            reset();
        }

        /**
         * 强制停止ScrollChecker，回调onPtrScrollAbort()，并重置了ScrollChecker状态 <p>
         * 在down动作发生时，如果ScrollChecker还没结束必须强制停止，
         * 而在onPtrScrollAbort()中只有是“自动刷新”&&“不在初始位置”才会回调onRelease(true)
         */
        public void abortIfWorking() {
            if (mIsRunning) {
                if (!mScroller.isFinished()) {
                    mScroller.forceFinished(true);
                }
                onPtrScrollAbort();
                reset();
            }
        }

        /**
         * 自动滑动到to位置
         */
        public void tryToScrollTo(int to, int duration) {
            if (mPtrIndicator.isAlreadyHere(to)) {
                return;//已经下拉到指定位置就直接return
            }
            mStart = mPtrIndicator.getCurrentPosY();
            mTo = to;
            int distance = to - mStart;
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "tryToScrollTo: start: %s, distance:%s, to:%s", mStart, distance, to);
            }
            removeCallbacks(this);

            mLastFlingY = 0;

            // fix #47: Scroller should be reused, https://github.com/liaohuqiu/android-Ultra-Pull-To-Refresh/issues/47
            if (!mScroller.isFinished()) {
                mScroller.forceFinished(true);//强制停止scroller
            }

            mScroller.startScroll(0, 0, 0, distance, duration);//start scroller
            post(this);//post runnable，对scroller滚动状态的检查
            mIsRunning = true;
        }
    }

    //////////////////////
    //配置变量
    //////////////////////

    /**
     * 配置计算器
     */
    public void setPtrIndicator(PtrIndicator slider) {
        if (mPtrIndicator != null && mPtrIndicator != slider) {
            slider.convertFrom(mPtrIndicator);
        }
        mPtrIndicator = slider;
    }

    /**
     * 横向滑动时，是否禁用下拉刷新
     */
    public void disableWhenHorizontalMove(boolean disable) {
        mDisableWhenHorizontalMove = disable;
    }

    /**
     * 最小刷新时间
     */
    public void setLoadingMinTime(int time) {
        mLoadingMinTime = time;
    }

    /**
     * @return 获取head view 高度
     */
    @SuppressWarnings("unused")
    public int getHeaderHeight() {
        return mHeaderHeight;
    }

    /**
     * 设置head view
     */
    public void setHeaderView(View header) {
        if (mHeaderView != null && header != null && mHeaderView != header) {
            removeView(mHeaderView);
        }
        ViewGroup.LayoutParams lp = header.getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(-1, -2);
            header.setLayoutParams(lp);
        }
        mHeaderView = header;
        addView(header);
    }

    /**
     * @return 获取head view
     */
    @SuppressWarnings({"unused"})
    public View getHeaderView() {
        return mHeaderView;
    }


    /**
     * @return 获取content view
     */
    @SuppressWarnings({"unused"})
    public View getContentView() {
        return mContent;
    }

    /**
     * The content view will now move when {@param pinContent} set to true.
     * 下拉是是否保持content位置不变
     *
     * @param pinContent
     */
    public void setPinContent(boolean pinContent) {
        if (pinContent) {
            mFlag = mFlag | FLAG_PIN_CONTENT;
        } else {
            mFlag = mFlag & ~FLAG_PIN_CONTENT;
        }
    }

    /**
     * 是否保持Coment位置不变
     */
    public boolean isPinContent() {
        return (mFlag & FLAG_PIN_CONTENT) > 0;
    }

    /**
     * 设置UI动画
     */
    public void setPtrHandler(PtrHandler ptrHandler) {
        mPtrHandler = ptrHandler;
    }

    /**
     * 添加UI动画
     */
    public void addPtrUIHandler(PtrUIHandler ptrUIHandler) {
        PtrUIHandlerHolder.addHandler(mPtrUIHandlerHolder, ptrUIHandler);
    }

    /**
     * 移除UI动画
     */
    @SuppressWarnings({"unused"})
    public void removePtrUIHandler(PtrUIHandler ptrUIHandler) {
        mPtrUIHandlerHolder = PtrUIHandlerHolder.removeHandler(mPtrUIHandlerHolder, ptrUIHandler);
    }


    /**
     * 设置阻尼系数
     *
     * @param resistance
     */
    public void setResistance(float resistance) {
        mPtrIndicator.setResistance(resistance);
    }

    /**
     * @return 阻尼系数
     */
    @SuppressWarnings({"unused"})
    public float getResistance() {
        return mPtrIndicator.getResistance();
    }

    /**
     * The duration to return back to the refresh position
     * 回弹到触发刷新阀值位置的时间
     *
     * @param duration
     */
    public void setDurationToClose(int duration) {
        mDurationToClose = duration;
    }

    /**
     * @return 回弹到触发刷新阀值位置的时间
     */
    @SuppressWarnings({"unused"})
    public float getDurationToClose() {
        return mDurationToClose;
    }

    /**
     * The duration to close time
     * 触发刷新阀值位置 到 0的时间
     *
     * @param duration
     */
    public void setDurationToCloseHeader(int duration) {
        mDurationToCloseHeader = duration;
    }

    /**
     * 触发刷新阀值位置 到 0的时间
     */
    @SuppressWarnings({"unused"})
    public long getDurationToCloseHeader() {
        return mDurationToCloseHeader;
    }

    /**
     * 设置head高度与触发刷新阀值的比例
     *
     * @param ratio
     */
    public void setRatioOfHeaderHeightToRefresh(float ratio) {
        mPtrIndicator.setRatioOfHeaderHeightToRefresh(ratio);
    }

    /**
     * @return head高度与触发刷新阀值的比例
     */
    @SuppressWarnings({"unused"})
    public float getRatioOfHeaderToHeightRefresh() {
        return mPtrIndicator.getRatioOfHeaderToHeightRefresh();
    }


    /**
     * 设置触发刷新阀值
     *
     * @param offset
     */
    @SuppressWarnings({"unused"})
    public void setOffsetToRefresh(int offset) {
        mPtrIndicator.setOffsetToRefresh(offset);
    }

    /**
     * @return 触发刷新阀值
     */
    public int getOffsetToRefresh() {
        return mPtrIndicator.getOffsetToRefresh();
    }

    /**
     * 设置刷新时head保持的高度
     *
     * @param offset
     */
    @SuppressWarnings({"unused"})
    public void setOffsetToKeepHeaderWhileLoading(int offset) {
        mPtrIndicator.setOffsetToKeepHeaderWhileLoading(offset);
    }

    /**
     * @return 刷新时head保持的高度
     */
    @SuppressWarnings({"unused"})
    public int getOffsetToKeepHeaderWhileLoading() {
        return mPtrIndicator.getOffsetToKeepHeaderWhileLoading();
    }

    /**
     * 设置刷新时是否保持头部
     *
     * @param keepOrNot
     */
    public void setKeepHeaderWhenRefresh(boolean keepOrNot) {
        mKeepHeaderWhenRefresh = keepOrNot;
    }

    /**
     * 刷新时是否保持头部
     */
    @SuppressWarnings({"unused"})
    public boolean isKeepHeaderWhenRefresh() {
        return mKeepHeaderWhenRefresh;
    }

    /**
     * 到达触发刷新阀值，是自动刷新还是释放才刷新，true自动刷新，false释放刷新
     *
     * @param pullToRefresh
     */
    public void setPullToRefresh(boolean pullToRefresh) {
        mPullToRefresh = pullToRefresh;
    }

    /**
     * @return 到达触发刷新阀值，是自动刷新还是释放才刷新 true自动刷新，false释放刷新
     */
    public boolean isPullToRefresh() {
        return mPullToRefresh;
    }


    /**
     * 弃用的方法
     *
     * @param yes
     */
    @Deprecated
    public void setInterceptEventWhileWorking(boolean yes) {
    }


    /////////////////////
    //创建自己的LayoutParams
    ////////////////////

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p != null && p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        @SuppressWarnings({"unused"})
        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }


    @SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions"})
    private boolean isDebug() {
        return DEBUG && DEBUG_LAYOUT;
    }

}
