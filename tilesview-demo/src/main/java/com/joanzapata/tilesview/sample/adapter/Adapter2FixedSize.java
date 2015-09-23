package com.joanzapata.tilesview.sample.adapter;

import android.content.Context;
import android.graphics.*;
import com.joanzapata.tilesview.adapter.FixedSizeAdapter;

import java.io.IOException;
import java.io.InputStream;

/**
 * Use the fixed size adapter to draw a picture with a resolution of 11730 x 6351.
 * You couldn't display this image in an Android ImageView if you wanted because it's
 * too large, and you downsampling it would make you loose all the details.
 * <br>
 * The TilesView combined with the Android BitmapRegionDecoder allows you to display
 * the whole image at an acceptable resolution, and display the details only where the
 * user is zooming, thus keeping a low memory footprint.
 * <br>
 * This is basically what the TilesView was made for.
 */
public class Adapter2FixedSize extends FixedSizeAdapter {

    private final BitmapRegionDecoder decoder;

    public Adapter2FixedSize(Context context) {
        super(11730, 6351);
        setOverscroll(300, 60, 90, 120);
        try {
            InputStream inputStream = context.getResources().getAssets().open("world.jpg");
            decoder = BitmapRegionDecoder.newInstance(inputStream, false);
        } catch (IOException e) {
            throw new RuntimeException("Please open an issue if you see this message.", e);
        }
    }

    @Override
    protected void drawTile(Canvas canvas, RectF sourceRectF, RectF destRect) {
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
