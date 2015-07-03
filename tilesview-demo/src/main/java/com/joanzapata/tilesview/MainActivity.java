package com.joanzapata.tilesview;

import android.app.Activity;
import android.graphics.*;
import android.os.Bundle;

public class MainActivity extends Activity {

    TilesView tilesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tilesView = (TilesView) findViewById(R.id.tilesView);

        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test);
        final TileRendererHelper helper = new TileRendererHelper(bitmap.getWidth(), bitmap.getHeight());
        final RectF sourceRectF = new RectF();
        final Rect destRect = new Rect();
        final Rect sourceRect = new Rect();
        final Paint paint = new Paint();
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
    }
}
