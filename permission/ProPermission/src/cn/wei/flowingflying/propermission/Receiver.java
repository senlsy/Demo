/**   
 * @Title: Receiver.java
 * @Package cn.wei.flowingflying.propermission
 * @Description: TODO
 * @author LinSQ
 * @date 2015-2-6 上午10:58:29
 * @version V1.0   
 */
package cn.wei.flowingflying.propermission;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * @ClassName: Receiver
 * @Description: TODO
 * @author LinSQ
 * @date 2015-2-6 上午10:58:29
 * 
 */
public class Receiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.e("lintest", "onReceive-------------");
		logIntent(intent);
	}

	public static void logIntent(Intent intent) {
		if (null != intent) {
			Bundle extras = intent.getExtras();
			if (null != extras && null != extras.keySet()) {
				for (String key : extras.keySet())
					Log.e("lintest", key + " : " + extras.get(key));
			}
		}
	}
}
