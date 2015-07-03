package com.joanzapata.tilesview;

import android.app.Activity;
import android.graphics.*;
import android.os.Bundle;
import com.joanzapata.tilesview.util.FixedImageSizeTileRenderer;
import com.joanzapata.tilesview.util.LayerOnFixedImageSize;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {

    TilesView tilesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tilesView = (TilesView) findViewById(R.id.tilesView);
        InputStream inputStream = null;

        try {
            inputStream = getResources().getAssets().open("world.jpg");
            final BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(inputStream, false);
            final Rect destRect = new Rect();
            final Rect sourceRect = new Rect();
            final Paint paint = new Paint();
            final Paint circlePaint = new Paint();
            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setColor(Color.RED);
            circlePaint.setAntiAlias(true);

            final int sourceWidth = decoder.getWidth();
            final int sourceHeight = decoder.getHeight();
            tilesView.setTileRenderer(new FixedImageSizeTileRenderer(sourceWidth, sourceHeight) {
                @Override
                public void renderTile(Canvas canvas, RectF sourceRectF) {
                    sourceRect.set(
                            (int) sourceRectF.left, (int) sourceRectF.top,
                            (int) sourceRectF.right, (int) sourceRectF.bottom
                    );
                    destRect.set(0, 0, canvas.getWidth(), canvas.getHeight());

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.outWidth = canvas.getWidth();
                    options.outHeight = canvas.getHeight();
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    options.inPreferQualityOverSpeed = true;
                    Bitmap tmpBitmap = decoder.decodeRegion(sourceRect, options);
                    canvas.drawBitmap(tmpBitmap, null, destRect, paint);
                    tmpBitmap.recycle();

                }
            });

            tilesView.addLayer(new LayerOnFixedImageSize(sourceWidth, sourceHeight) {
                @Override
                public void renderLayer(Canvas canvas) {
                    canvas.drawCircle(
                            scaled(570),
                            scaled(281),
                            scaled(15), circlePaint);
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
}
