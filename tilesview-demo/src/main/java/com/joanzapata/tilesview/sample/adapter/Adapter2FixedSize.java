package com.joanzapata.tilesview.sample.adapter;

import android.content.Context;
import android.graphics.*;
import com.joanzapata.tilesview.adapter.FixedSizeAdapter;

import java.io.IOException;
import java.io.InputStream;

public class Adapter2FixedSize extends FixedSizeAdapter {

    private final BitmapRegionDecoder decoder;

    public Adapter2FixedSize(Context context) {
        super(11730, 6351);
        try {
            InputStream inputStream = context.getResources().getAssets().open("world.jpg");
            decoder = BitmapRegionDecoder.newInstance(inputStream, false);
        } catch (IOException e) {
            throw new RuntimeException("Please open an issue if you see this message.", e);
        }
    }

    @Override
    protected void renderTile(Canvas canvas, RectF sourceRectF, RectF destRect) {
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
}
