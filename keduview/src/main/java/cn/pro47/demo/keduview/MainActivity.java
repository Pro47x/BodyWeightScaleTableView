package cn.pro47.demo.keduview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    BodyWeightScaleTableView mBodyWeightScaleTableView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBodyWeightScaleTableView = (BodyWeightScaleTableView) findViewById(R.id.kedu);
    }

    public void test(View view) {
        mBodyWeightScaleTableView.setBodyWeight(80 * 1000, true);
    }
}
