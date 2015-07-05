package com.joanzapata.tilesview.util;

import android.graphics.Canvas;
import com.joanzapata.tilesview.Layer;

public abstract class LayerOnFixedImageSize implements Layer {

    private final float sourceWidth, sourceHeight;

    private float sourceInitialRatio, scale;

    public LayerOnFixedImageSize(float sourceWidth, float sourceHeight) {
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    @Override
    public final void renderLayer(Canvas canvas, float scale,
            float contentInitialWidth, float contentInitialHeight) {

        // Try using source width as reference
        float translateX, translateY;
        float scaledSourceHeight = contentInitialWidth * sourceHeight / sourceWidth;
        float sourceRatioOnZoom1;
        if (scaledSourceHeight <= contentInitialHeight) {
            sourceRatioOnZoom1 = contentInitialWidth / sourceWidth;
            translateX = 0;
            translateY = (contentInitialHeight - sourceHeight * sourceRatioOnZoom1) / 2f * scale;
        } else {
            sourceRatioOnZoom1 = contentInitialHeight / sourceHeight;
            translateX = (contentInitialWidth - sourceWidth * sourceRatioOnZoom1) / 2f * scale;
            translateY = 0;
        }

        canvas.translate(translateX, translateY);

        this.scale = scale;
        this.sourceInitialRatio = sourceRatioOnZoom1;
        renderLayer(canvas);
    }

    protected float scaled(float pixelSizeOnSourceImage) {
        return pixelSizeOnSourceImage * sourceInitialRatio * scale;
    }

    protected abstract void renderLayer(Canvas canvas);

}
