package com.joanzapata.tilesview.sample.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.widget.Toast;
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
    private final Context context;

    public Adapter3FixedSizeEnhanced(Context context) {
        super(context);
        this.context = context;

        // POIs are placed using X and Y coordinates relatively to the huge map picture.
        // The anchor point is centered on X but the Y value depends on the POIs bitmap.
        pois = Arrays.asList(
                new POI("Tajmahal", context, R.drawable.tajmahal, 7876f, 2400f, 5 / 7f),
                new POI("Big Ben", context, R.drawable.bigben, 5500f, 1531.5f, 4 / 5f),
                new POI("Eiffel Tower", context, R.drawable.eiffel, 5563.5f, 1623.5f, 4 / 5f),
                new POI("Coliseum", context, R.drawable.colosseum, 5849f, 1870f, 2 / 3f),
                new POI("Egypt", context, R.drawable.egypt, 6427.5f, 2296.5f, 2 / 3f),
                new POI("Statue of Liberty", context, R.drawable.liberty, 3318.5f, 1912f, 4 / 5f));
    }

    /**
     * Draws every POI at their position. Note the use of {@link #scaled(float)}
     * which is provided by {@link Adapter2FixedSize} and convert any offset value
     * from the original image to the actual pixel size.
     */
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

    /**
     * When a POI is clicked, a toast shows its name. X and Y are already
     * relative to the huge map image, so all we have to do here is to check
     * that the POI contains the given X and Y. Scale is still useful to
     * determine the actual hit box of the POI, because if the map is completely
     * zoomed out at 1:200 for example, the user might click 1px to the left of a POI,
     * which would be translated in 200px to the left on the map.
     */
    @Override
    public void onClick(float x, float y, float scale) {
        for (int i = 0, poisSize = pois.size(); i < poisSize; i++) {
            POI poi = pois.get(i);
            if (poi.contains(x, y, scale)) {
                Toast.makeText(context, poi.name, Toast.LENGTH_LONG).show();
                break;
            }
        }
    }
}
