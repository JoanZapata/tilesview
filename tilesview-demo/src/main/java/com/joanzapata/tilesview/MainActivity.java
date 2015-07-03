package com.joanzapata.tilesview;

import android.app.Activity;
import android.graphics.*;
import android.os.Bundle;
import com.joanzapata.tilesview.util.FixedImageSizeTileRenderer;
import com.joanzapata.tilesview.util.LayerOnFixedImageSize;

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
            final Rect destRect = new Rect();
            final Rect sourceRect = new Rect();
            final Paint paint = new Paint();
            final Paint circlePaint = new Paint();
            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setColor(Color.RED);
            circlePaint.setAntiAlias(true);

            tilesView.setTileRenderer(new FixedImageSizeTileRenderer(bitmap.getWidth(), bitmap.getHeight()) {
                @Override
                public void renderTile(Canvas canvas, RectF sourceRectF) {
                    sourceRect.set(
                            (int) sourceRectF.left, (int) sourceRectF.top,
                            (int) sourceRectF.right, (int) sourceRectF.bottom
                    );
                    destRect.set(0, 0, canvas.getWidth(), canvas.getHeight());
                    canvas.drawBitmap(bitmap, sourceRect, destRect, paint);
                }
            });

            tilesView.addLayer(new LayerOnFixedImageSize(bitmap.getWidth(), bitmap.getHeight()) {
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
        }
    }
}
