package com.joanzapata.tilesview.sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.joanzapata.tilesview.TilesView;
import com.joanzapata.tilesview.TilesViewAdapter;
import com.joanzapata.tilesview.adapter.FixedSizeAdapter;

import java.lang.reflect.Constructor;

import static android.widget.Toast.LENGTH_LONG;

public class BaseDemoActivity extends Activity {

    private static final String TAG = BaseDemoActivity.class.getSimpleName();
    private static final String EXTRA_ADAPTER = "EXTRA_ADAPTER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TilesView tilesView = new TilesView(this);
        tilesView.setBackgroundColor(Color.BLACK);
        tilesView.setPadding(30, 50, 70, 90);
        tilesView.setContentPadding(30, 50, 70, 90);
        tilesView.setDebug(true);
        setContentView(tilesView);

        String className = getIntent().getStringExtra(EXTRA_ADAPTER);
        try {
            Class<?> clazz = Class.forName(className);
            Constructor constructor = clazz.getConstructor(Context.class);
            TilesViewAdapter adapter = (TilesViewAdapter) constructor.newInstance(this);
            tilesView.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Unable to instantiate adapter " + className);
            Toast.makeText(this, "Oops! There was a problem with this one.", LENGTH_LONG).show();
            finish();
        }
    }

    public static Intent newIntent(Context context, Class<? extends TilesViewAdapter> adapter) {
        Intent intent = new Intent(context, BaseDemoActivity.class);
        intent.putExtra(EXTRA_ADAPTER, adapter.getCanonicalName());
        return intent;
    }
}
