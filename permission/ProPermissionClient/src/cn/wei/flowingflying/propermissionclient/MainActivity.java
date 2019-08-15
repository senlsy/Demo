package cn.wei.flowingflying.propermissionclient;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

	TextView tv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tv = (TextView) this.findViewById(R.id.tv);
	}

	public void onClickAction(View v) {
		if (v.getId() == R.id.button1) {
			Intent intent = new Intent();
			intent.setClassName("cn.wei.flowingflying.propermission","cn.wei.flowingflying.propermission.PrivActivity");
			startActivity(intent);
		} else if (v.getId() == R.id.button2) {
			Uri uri = Uri.parse("content://cn.wei.flowingflying.propermission.PrivProvider/hello/1");
			readContent(uri);
		} else if (v.getId() == R.id.button3) {
			Intent intent = new Intent();
			intent.setClassName("com.example.propermissiongrant","com.example.propermissiongrant.MainActivity");
			intent.setData(Uri.parse("content://cn.wei.flowingflying.propermission.PrivProvider/hello/1"));
			intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivity(intent);
		} else if (v.getId() == R.id.button4) {
			getApplicationContext().sendBroadcast(new Intent("sen.receiver"));

		}
	}

	private void readContent(Uri uri) {
		ContentResolver cr = getContentResolver();
		Cursor c = cr.query(uri, null, null, null, null);
		c.moveToFirst();
		int num = c.getInt(c.getColumnIndex("Number"));
		tv.append("读取Content Provider：\nuri = " + uri.toString() + "\n num = "	+ num);
	}

}
