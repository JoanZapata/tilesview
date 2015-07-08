package com.joanzapata.tilesview;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
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

    TilesView tilesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new DebugDrawer()
                .elements("UI",
                        new AnimationSpeedElement(),
                        new LeakCanaryElement(),
                        new RiseAndShineElement(),
                        new ToggleElement("Show grid", false, true) {
                            @Override
                            public void onSwitch(boolean b) {
                                tilesView.setDebug(b);
                            }
                        })
                .modules(
                        new BuildModule(),
                        new DeviceInfoModule(),
                        new MadgeModule(),
                        new ScalpelModule(),
                        new GhostModule())
                .bind(this);

        tilesView = (TilesView) findViewById(R.id.tilesView);
        InputStream inputStream = null;

        try {
            inputStream = getResources().getAssets().open("world.jpg");
            final BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(inputStream, false);

            final Paint paint = new Paint();
            final int sourceWidth = decoder.getWidth();
            final int sourceHeight = decoder.getHeight();
            tilesView.setDebug(false);
            tilesView.setTileRenderer(new FixedImageSizeTileRenderer(sourceWidth, sourceHeight) {
                @Override
                public void renderTile(Canvas canvas, RectF sourceRectF, RectF destRect) {
                    Rect sourceRect = new Rect(
                            (int) sourceRectF.left, (int) sourceRectF.top,
                            (int) sourceRectF.right, (int) sourceRectF.bottom);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    options.inPreferQualityOverSpeed = true;
                    Bitmap tmpBitmap = decoder.decodeRegion(sourceRect, options);
                    canvas.drawBitmap(tmpBitmap, null, destRect, paint);
                    tmpBitmap.recycle();

                }
            });

            final List<POI> pois = Arrays.asList(
                    new POI(this, R.drawable.tajmahal, 3918, 990, 5 / 7f),
                    new POI(this, R.drawable.bigben, 2724.5f, 552f, 4 / 5f),
                    new POI(this, R.drawable.eiffel, 2756.5f, 598, 4 / 5f),
                    new POI(this, R.drawable.colosseum, 2898.5f, 721.5f, 2 / 3f),
                    new POI(this, R.drawable.egypt, 3188, 935, 2 / 3f),
                    new POI(this, R.drawable.liberty, 1636, 742, 4 / 5f));
            tilesView.addLayer(new LayerOnFixedImageSize(sourceWidth, sourceHeight) {
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

        public POI(Context context, int bitmapRes, float offsetX, float offsetY, float yAnchorRatio) {
            bitmap = BitmapFactory.decodeResource(context.getResources(), bitmapRes);
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.deltaX = -bitmap.getWidth() * 0.5f;
            this.deltaY = -bitmap.getHeight() * yAnchorRatio;
        }

    }
}
