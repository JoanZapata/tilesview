package com.joanzapata.tilesview;

import android.app.Activity;
import android.graphics.*;
import android.os.Bundle;
import com.joanzapata.tilesview.TileRendererHelper.SizeConverter;

import java.io.IOException;

public class MainActivity extends Activity {

    TilesView tilesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tilesView = (TilesView) findViewById(R.id.tilesView);

        final Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(getResources().getAssets().open("world.jpg"));
            final TileRendererHelper helper = new TileRendererHelper(bitmap.getWidth(), bitmap.getHeight());
            final RectF sourceRectF = new RectF();
            final Rect destRect = new Rect();
            final Rect sourceRect = new Rect();
            final Paint paint = new Paint();
            final Paint circlePaint = new Paint();
            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setColor(Color.RED);
            circlePaint.setAntiAlias(true);

            tilesView.setTileRenderer(new TilesView.TileRenderer() {
                @Override
                public void renderTile(Canvas canvas, float x, float y,
                        float width, float height,
                        float overallWidth, float overallHeight) {

                    // FitCenter the image on the screen
                    helper.computeSourceRect(sourceRectF, x, y, width, height, overallWidth, overallHeight);
                    sourceRect.set(
                            (int) sourceRectF.left, (int) sourceRectF.top,
                            (int) sourceRectF.right, (int) sourceRectF.bottom
                    );

                    // Draw on the given canvas
                    destRect.set(0, 0, canvas.getWidth(), canvas.getHeight());
                    canvas.drawBitmap(bitmap, sourceRect, destRect, paint);

                }
            });

            tilesView.addLayer(new TilesView.Layer() {
                @Override
                public void renderLayer(Canvas canvas, float scale, float overallWidth, float overallHeight) {
                    SizeConverter c = helper.translateCanvasAndGetConverter(canvas, scale, overallWidth, overallHeight);

                    canvas.drawCircle(
                            c.convert(570),
                            c.convert(281),
                            c.convert(15), circlePaint);

                    canvas.drawCircle(
                            c.convert(0),
                            c.convert(0),
                            20, circlePaint);

                    canvas.drawCircle(
                            c.convert(1200),
                            c.convert(715),
                            20, circlePaint);
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
        ;
    }
}
