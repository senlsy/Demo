package in.srain.cube.views.ptr;

/**
 * 钩子任务类，实现了 Runnable 接口，可以理解为在原来的操作之间，插入了一段任务去执行。<br>
 * 1、一个钩子任务只能执行一次，通过调用 takeOver() 去执行。执行的是自身的run方法<br>
 * 2、用户需要调用手动调用 resume() 方法，去恢复执行原来的操作。执行的是mResumeAction的run方法<br>
 * 3、如果钩子任务已经执行过了，再次调用 takeOver() 将会直接恢复执行原来的操作。执行的是mResumeAction的run方法<br>
 * <p>
 * 可以通过 PtrFrameLayout 类的 setRefreshCompleteHook(PtrUIHandlerHook hook) 进行设置。
 * 当用户调用 refreshComplete() 方法表示刷新结束以后，
 * 如果有 hook 存在，先执行 hook 的 takeOver 方法，执行结束，
 * 用户需要主动调用 hook 的 resume 方法，然后才会进行 Header 回弹到顶部的动作。
 */
public abstract class PtrUIHandlerHook implements Runnable {

    private Runnable mResumeAction;
    private static final byte STATUS_PREPARE = 0;//未执行自身的run
    private static final byte STATUS_IN_HOOK = 1;//已执行过自身run
    private static final byte STATUS_RESUMED = 2;//已执行过mResumeAction的run
    private byte mStatus = STATUS_PREPARE;

    public void takeOver() {
        takeOver(null);
    }

    /**
     * 1、未执行自身的run状态，将 mStatus 设置为已执行过自身run状态，并自行自身run <br></>
     * 2、已执行过自身run状态，就什么都不干
     * 3、已执行过mResumeAction的run状态，就继续执行mResumeAction的run
     */
    public void takeOver(Runnable resumeAction) {

        if (resumeAction != null) {
            mResumeAction = resumeAction;
        }

        switch (mStatus) {
            case STATUS_PREPARE:
                mStatus = STATUS_IN_HOOK;//只能执行自身方法一次
                run();
                break;
            case STATUS_IN_HOOK:
                break;
            case STATUS_RESUMED:
                resume();
                break;
        }
    }

    /**
     * 重置回未执行自身的run的状态
     */
    public void reset() {
        mStatus = STATUS_PREPARE;
    }

    /**
     * 执行mResumeAction.run()，将mStatus设置为已执行过mResumeAction的run状态
     */
    public void resume() {
        if (mResumeAction != null) {
            mResumeAction.run();
        }
        mStatus = STATUS_RESUMED;
    }

    /**
     * Hook should always have a resume action, which is hooked by this hook.
     *
     * @param runnable
     */
    public void setResumeAction(Runnable runnable) {
        mResumeAction = runnable;
    }
}