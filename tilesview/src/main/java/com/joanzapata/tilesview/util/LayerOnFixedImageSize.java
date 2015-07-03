package com.joanzapata.tilesview.util;

import android.graphics.Canvas;
import com.joanzapata.tilesview.Layer;

public abstract class LayerOnFixedImageSize implements Layer {

    private final float sourceWidth, sourceHeight;

    private float sourceRatioOnZoom1, scale;

    public LayerOnFixedImageSize(float sourceWidth, float sourceHeight) {
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    @Override
    public final void renderLayer(Canvas canvas, float scale, float overallWidth, float overallHeight) {

        // Try using source width as reference
        float translateX, translateY;
        float scaledSourceHeight = overallWidth * sourceHeight / sourceWidth;
        float sourceRatioOnZoom1;
        if (scaledSourceHeight <= overallHeight) {
            sourceRatioOnZoom1 = overallWidth / sourceWidth;
            translateX = 0;
            translateY = (overallHeight - sourceHeight * sourceRatioOnZoom1) / 2f * scale;
        } else {
            sourceRatioOnZoom1 = overallHeight / sourceHeight;
            translateX = (overallWidth - sourceWidth * sourceRatioOnZoom1) / 2f * scale;
            translateY = 0;
        }

        canvas.translate(translateX, translateY);

        this.scale = scale;
        this.sourceRatioOnZoom1 = sourceRatioOnZoom1;
        renderLayer(canvas);
    }

    protected float scaled(float pixelSizeOnSourceImage) {
        return pixelSizeOnSourceImage * sourceRatioOnZoom1 * scale;
    }

    protected abstract void renderLayer(Canvas canvas);


}
