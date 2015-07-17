package com.joanzapata.tilesview;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.widget.SeekBar;
import com.joanzapata.tilesview.util.FixedImageSizeAnimator;
import com.joanzapata.tilesview.util.FixedImageSizeTappedListener;
import com.joanzapata.tilesview.util.FixedImageSizeTileRenderer;
import com.joanzapata.tilesview.util.LayerOnFixedImageSize;
import com.jug6ernaut.debugdrawer.DebugDrawer;
import com.jug6ernaut.debugdrawer.views.ToggleElement;
import com.jug6ernaut.debugdrawer.views.elements.AnimationSpeedElement;
import com.jug6ernaut.debugdrawer.views.elements.LeakCanaryElement;
import com.jug6ernaut.debugdrawer.views.elements.RiseAndShineElement;
import com.jug6ernaut.debugdrawer.views.modules.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {

    private static final int MIN_ZOOM_LEVEL = 10;
    private static final int MAX_ZOOM_LEVEL = 20;

    TilesView tilesView;

    SeekBar seekBar;

    private boolean isTrackingTouchOnSeekBar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ToggleElement toggleElement = new ToggleElement("Show grid", false, true) {
            @Override
            public void onSwitch(boolean b) {
                tilesView.setDebug(b);
            }
        };

        int contentPadding = getResources().getDimensionPixelSize(R.dimen.content_padding);

        new DebugDrawer()
                .elements("UI",
                        new AnimationSpeedElement(),
                        new LeakCanaryElement(),
                        new RiseAndShineElement(),
                        toggleElement)
                .modules(
                        new BuildModule(),
                        new DeviceInfoModule(),
                        new MadgeModule(),
                        new ScalpelModule(),
                        new GhostModule())
                .bind(this);

        tilesView = (TilesView) findViewById(R.id.tilesView);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        InputStream inputStream = null;

        try {
            inputStream = getResources().getAssets().open("world.jpg");
            final BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(inputStream, false);

            final List<POI> pois = Arrays.asList(
                    new POI("Tajmahal", this, R.drawable.tajmahal, 7876f, 2400f, 5 / 7f),
                    new POI("Big Ben", this, R.drawable.bigben, 5500f, 1531.5f, 4 / 5f),
                    new POI("Eiffel Tower", this, R.drawable.eiffel, 5563.5f, 1623.5f, 4 / 5f),
                    new POI("Coliseum", this, R.drawable.colosseum, 5849f, 1870f, 2 / 3f),
                    new POI("Egypt", this, R.drawable.egypt, 6427.5f, 2296.5f, 2 / 3f),
                    new POI("Statue of Liberty", this, R.drawable.liberty, 3318.5f, 1912f, 4 / 5f));

            final int sourceWidth = decoder.getWidth();
            final int sourceHeight = decoder.getHeight();
            seekBar.setMax(MAX_ZOOM_LEVEL - MIN_ZOOM_LEVEL);
            final FixedImageSizeAnimator animator = new FixedImageSizeAnimator(tilesView, sourceWidth, sourceHeight);
            tilesView.setDebug(toggleElement.isChecked())
                    .setMinZoomLevel(MIN_ZOOM_LEVEL)
                    .setMaxZoomLevel(MAX_ZOOM_LEVEL)
                    .setContentPadding(contentPadding, contentPadding, contentPadding, contentPadding)
                    .setTileRenderer(new FixedImageSizeTileRenderer(sourceWidth, sourceHeight) {
                        @Override
                        public void renderTile(Canvas canvas, RectF sourceRectF, RectF destRect) {
                            Rect sourceRect = new Rect(
                                    (int) sourceRectF.left, (int) sourceRectF.top,
                                    (int) sourceRectF.right, (int) sourceRectF.bottom);
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inPreferredConfig = Bitmap.Config.RGB_565;
                            options.inPreferQualityOverSpeed = true;
                            Bitmap tmpBitmap = decoder.decodeRegion(sourceRect, options);
                            canvas.drawBitmap(tmpBitmap, null, destRect, null);
                            tmpBitmap.recycle();
                        }
                    })
                    .addLayer(new LayerOnFixedImageSize(sourceWidth, sourceHeight) {
                        @Override
                        public void renderLayer(Canvas canvas) {
                            for (int i = 0; i < pois.size(); i++) {
                                POI poi = pois.get(i);
                                canvas.drawBitmap(poi.bitmap,
                                        scaled(poi.offsetX) + poi.deltaX,
                                        scaled(poi.offsetY) + poi.deltaY,
                                        null);
                            }
                        }
                    })
                    .setOnContentTappedListener(new FixedImageSizeTappedListener(sourceWidth, sourceHeight) {
                        @Override
                        protected void contentTapped(float x, float y, float scale) {
                            for (int i = pois.size() - 1; i >= 0; i--) {
                                POI poi = pois.get(i);
                                if (poi.contains(x, y, scale)) {
                                    Snackbar.make(tilesView, "Tapped " + poi.name, Snackbar.LENGTH_LONG).show();
                                    animator.animateTo(poi.offsetX, poi.offsetY, 18);
                                    return;
                                }
                            }
                        }
                    })
                    .setOnZoomLevelChangedListener(new OnZoomLevelChangedListener() {
                        @Override
                        public void onZoomLevelChanged(int zoomLevel) {
                            if (!isTrackingTouchOnSeekBar)
                                seekBar.setProgress(zoomLevel - MIN_ZOOM_LEVEL);
                        }
                    });

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        tilesView.setZoomLevel(MIN_ZOOM_LEVEL + progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    isTrackingTouchOnSeekBar = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    isTrackingTouchOnSeekBar = false;
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            quietClose(inputStream);
        }
    }

    private void quietClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Quiet
            }
        }
    }

    private class POI {

        final Bitmap bitmap;

        final float offsetX, offsetY;

        final float deltaX, deltaY;

        final String name;

        public POI(String name, Context context, int bitmapRes, float offsetX, float offsetY, float yAnchorRatio) {
            this.bitmap = BitmapFactory.decodeResource(context.getResources(), bitmapRes);
            this.deltaX = -bitmap.getWidth() * 0.5f;
            this.deltaY = -bitmap.getHeight() * yAnchorRatio;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.name = name;
        }

        public boolean contains(float x, float y, float scale) {
            float distance = (float) Math.sqrt(
                    (offsetX - x) * (offsetX - x) +
                            (offsetY - y) * (offsetY - y));
            return distance <= bitmap.getWidth() / scale / 2f;
        }
    }
}
