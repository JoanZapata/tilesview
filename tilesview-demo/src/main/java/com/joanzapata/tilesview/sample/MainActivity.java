package com.joanzapata.tilesview.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import com.joanzapata.tilesview.sample.adapter.Adapter1Base;
import com.joanzapata.tilesview.sample.adapter.Adapter2FixedSize;
import com.joanzapata.tilesview.sample.adapter.Adapter3FixedSizeEnhanced;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.sample1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(BaseDemoActivity.newIntent(MainActivity.this, Adapter1Base.class));
            }
        });

        findViewById(R.id.sample2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(BaseDemoActivity.newIntent(MainActivity.this, Adapter2FixedSize.class));
            }
        });

        findViewById(R.id.sample3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(BaseDemoActivity.newIntent(MainActivity.this, Adapter3FixedSizeEnhanced.class));
            }
        });
    }
}
