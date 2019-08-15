package in.srain.cube.views.ptr;

import android.view.View;

/**
* 类描述：刷新事件回调接口，及检查content是否可开始下拉刷新
* 创建人：mark.lin
* 创建时间：2016/12/30 9:46
* 修改备注：
* @version
*
*/
public interface PtrHandler {

    /**
     * Check can do refresh or not. For example the content is empty or the first child is in view.
     * <p/>
     * {@link in.srain.cube.views.ptr.PtrDefaultHandler#checkContentCanBePulledDown}
     */
    boolean checkCanDoRefresh(final PtrFrameLayout frame, final View content, final View header);

    /**
     * When refresh begin
     *
     * @param frame
     */
    void onRefreshBegin(final PtrFrameLayout frame);
}