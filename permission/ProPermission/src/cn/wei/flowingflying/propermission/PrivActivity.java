package cn.wei.flowingflying.propermission;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;


public class PrivActivity extends Activity
{
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout view=new LinearLayout(this);
        view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        view.setOrientation(LinearLayout.HORIZONTAL);
        TextView tv1=new TextView(this);
        tv1.setText(Html.fromHtml("<b>Hello from PrivActivity...<b>山顶的朋友，你好吗..."));
        view.addView(tv1);
        setContentView(view);
    }
}
