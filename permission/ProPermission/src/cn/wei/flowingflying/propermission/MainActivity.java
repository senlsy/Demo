package cn.wei.flowingflying.propermission;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;


public class MainActivity extends Activity
{
    
    TextView tv;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv=(TextView)this.findViewById(R.id.tv);
        getInfoFromProvider();
        this.findViewById(R.id.btn).setOnClickListener(new OnClickListener(){
            
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), PrivActivity.class));
            }
        });
        this.findViewById(R.id.btn2).setOnClickListener(new OnClickListener(){
            
            @Override
            public void onClick(View v) {
                getApplicationContext().sendBroadcast(new Intent("sen.receiver"));
            }
        });
    }
    
    private void getInfoFromProvider() {
        Uri uri=PrivProvider.CONTENT_URI;
        ContentResolver cr=getContentResolver();
        Cursor c=cr.query(uri, null, null, null, null);
        c.moveToFirst();
        int num=c.getInt(c.getColumnIndex(PrivProvider.colsName[0]));
        tv.setText("Read from Provider : num = " + num);
        Log.d("lintest", "Read from Provider : num = " + num);
    }
}
