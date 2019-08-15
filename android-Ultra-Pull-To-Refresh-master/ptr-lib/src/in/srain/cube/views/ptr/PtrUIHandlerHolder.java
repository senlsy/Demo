package in.srain.cube.views.ptr;

import in.srain.cube.views.ptr.indicator.PtrIndicator;

/**
 * PtrUIHandler的holder对象，并构建PtrUIHandlerHolder责任链。
 * 执行节点的操作方法时，递归调用该节点之后每个节点的相同操作方法。
 * <p>
 * <p>
 * 目的是：让链表中实现PtrUIHandler的所有对象，都能执行自己的动画
 */
class PtrUIHandlerHolder implements PtrUIHandler {

    private PtrUIHandler mHandler;
    private PtrUIHandlerHolder mNext;

    /////////
    //构建责任链
    /////////
    public static PtrUIHandlerHolder create() {
        return new PtrUIHandlerHolder();
    }

    /**
     * 将handler节点包装成PtrUIHandlerHolder，并添加进head链表中
     */
    public static void addHandler(PtrUIHandlerHolder head, PtrUIHandler handler) {

        if (null == handler || head == null) {
            return;
        }

        if (null == head.mHandler) {
            head.mHandler = handler;
            return;
        }

        PtrUIHandlerHolder current = head;
        for (; ; current = current.mNext) {//将current指向链尾
            if (current.contains(handler)) {
                //加入的handler已经存在链表中
                return;
            }
            if (current.mNext == null) {
                //链表末端
                break;
            }
        }
        //接到链尾
        PtrUIHandlerHolder newHolder = new PtrUIHandlerHolder();
        newHolder.mHandler = handler;
        current.mNext = newHolder;

    }


    /**
     * 从head这条链中删除handler节点
     *
     * @return 返回的是head链中的头节点
     */
    public static PtrUIHandlerHolder removeHandler(PtrUIHandlerHolder head, PtrUIHandler handler) {

        if (head == null || handler == null || head.mHandler == null) {
            return head;
        }

        PtrUIHandlerHolder current = head;
        PtrUIHandlerHolder pre = null;
        do {

            // delete current: link pre to next, unlink next from current;
            // pre will no change, current move to next element;
            if (current.contains(handler)) {

                // current is head
                if (pre == null) {

                    head = current.mNext;
                    current.mNext = null;
                    current = head;

                } else {

                    pre.mNext = current.mNext;
                    current.mNext = null;
                    current = pre.mNext;
                }
            } else {
                pre = current;
                current = current.mNext;
            }

        } while (current != null);

        if (head == null) {
            head = new PtrUIHandlerHolder();
        }
        return head;
    }

    /////////////////////////////
    //自身方法
    //////////////////////////

    private PtrUIHandlerHolder() {

    }

    private boolean contains(PtrUIHandler handler) {
        return mHandler != null && mHandler == handler;
    }

    public boolean hasHandler() {
        return mHandler != null;
    }

    private PtrUIHandler getHandler() {
        return mHandler;
    }

    //////////////////////////
    //PtrUIHandler的包装方法，执行某节点的操作方法时，递归调用该节点之后每个节点的相同操作方法
    /////////////////////////

    @Override
    public void onUIReset(PtrFrameLayout frame) {
        PtrUIHandlerHolder current = this;
        do {
            final PtrUIHandler handler = current.getHandler();
            if (null != handler) {
                handler.onUIReset(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshPrepare(PtrFrameLayout frame) {
        if (!hasHandler()) {
            return;
        }
        PtrUIHandlerHolder current = this;
        do {
            final PtrUIHandler handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshPrepare(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshBegin(PtrFrameLayout frame) {
        PtrUIHandlerHolder current = this;
        do {
            final PtrUIHandler handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshBegin(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshComplete(PtrFrameLayout frame) {
        PtrUIHandlerHolder current = this;
        do {
            final PtrUIHandler handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshComplete(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIPositionChange(PtrFrameLayout frame, boolean isUnderTouch, byte status, PtrIndicator ptrIndicator) {
        PtrUIHandlerHolder current = this;
        do {
            final PtrUIHandler handler = current.getHandler();
            if (null != handler) {
                handler.onUIPositionChange(frame, isUnderTouch, status, ptrIndicator);
            }
        } while ((current = current.mNext) != null);
    }
}
