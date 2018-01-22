package com.bearpot.artest.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.bearpot.artest.R;
import com.bearpot.artest.app.ObjectRecognition.ObjectTargets;

/**
 * Created by dg.jung on 2018-01-22.
 */

public class ActivityLauncher extends Activity {
    private Button object_detect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        object_detect = (Button) findViewById(R.id.Object_Detect);

        object_detect.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ActivityLauncher.this, ObjectTargets.class);
                startActivity(intent);
            }
        });
    }
}
