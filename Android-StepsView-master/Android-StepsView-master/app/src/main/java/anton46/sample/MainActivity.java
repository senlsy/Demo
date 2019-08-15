package anton46.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.jia.stepsview.StepsView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    
    private final String[] labels = {"Step 1","Step 2","Step 3","Step 4","Step 5"};
    private final String[] labels2 = {"Step 1","Step 2","Step 3","Step 4","Step 5","Step 6","Step 7"};
    StepsView stepsView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stepsView = (StepsView)this.findViewById(R.id.stepsView);
        stepsView.setCompletedPosition( -1)
                 .setLabels(labels).setBarColorIndicator(getResources().getColor(R.color.material_blue_grey_800))
                 .setProgressColorIndicator(getResources().getColor(R.color.orange))
                 .setLabelColorComplete(getResources().getColor(R.color.orange))
                 .setLabelColorIndicator(getResources().getColor(R.color.material_blue_grey_800))
                 .drawView();
    }
    
    int i = 0;
    
    @Override
    public void onClick(View v) {
        stepsView.setCompletedPosition(5)
                 .setLabels(labels2)
                 .drawView();
        i = i % 3;
    }
}
