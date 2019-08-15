package com.example.propermissiongrant;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

	private TextView tv = null;
	private static String AUTHORITY = "cn.wei.flowingflying.propermission.PrivProvider";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tv = (TextView) findViewById(R.id.tv);
		Intent intent = getIntent();
		if (intent != null) {
			Uri uri = intent.getData();
			if (uri != null && uri.getAuthority().equals(AUTHORITY)) {
				readContent(uri);
			}
		}
	}

	private void readContent(Uri uri) {
		try {
			ContentResolver cr = getContentResolver();
			Cursor c = cr.query(uri, null, null, null, null);
			c.moveToFirst();
			int num = c.getInt(c.getColumnIndex("Number"));
			tv.append("\n读取Content Provider：\nuri = " + uri.toString()
					+ "\n num = " + num);
		} catch (Exception e) {
			tv.append("\nERROR: \n" + e.toString());
		}
	}
}
