package com.joanzapata.tilesview.sample.adapter;

import android.content.Context;
import android.graphics.Canvas;
import com.joanzapata.tilesview.sample.R;
import com.joanzapata.tilesview.sample.utils.POI;

import java.util.Arrays;
import java.util.List;

/**
 * Same as {@link Adapter2FixedSize} but also
 * display some items in a layer.
 */
public class Adapter3FixedSizeEnhanced extends Adapter2FixedSize {

    private final List<POI> pois;

    public Adapter3FixedSizeEnhanced(Context context) {
        super(context);
        pois = Arrays.asList(
                new POI("Tajmahal", context, R.drawable.tajmahal, 7876f, 2400f, 5 / 7f),
                new POI("Big Ben", context, R.drawable.bigben, 5500f, 1531.5f, 4 / 5f),
                new POI("Eiffel Tower", context, R.drawable.eiffel, 5563.5f, 1623.5f, 4 / 5f),
                new POI("Coliseum", context, R.drawable.colosseum, 5849f, 1870f, 2 / 3f),
                new POI("Egypt", context, R.drawable.egypt, 6427.5f, 2296.5f, 2 / 3f),
                new POI("Statue of Liberty", context, R.drawable.liberty, 3318.5f, 1912f, 4 / 5f));
    }

    @Override
    public void drawLayer(Canvas canvas, float scale) {
        for (int i = 0, size = pois.size(); i < size; i++) {
            POI poi = pois.get(i);
            canvas.drawBitmap(poi.bitmap,
                    scaled(poi.offsetX) + poi.deltaX,
                    scaled(poi.offsetY) + poi.deltaY,
                    null);
        }
    }

}
