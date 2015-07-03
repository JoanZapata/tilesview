package com.joanzapata.tilesview;

import android.graphics.Canvas;
import android.graphics.RectF;

public class TileRendererHelper {

    private final float sourceWidth, sourceHeight;

    private final SizeConverter sizeConverter = new SizeConverter();

    public TileRendererHelper(int sourceWidth, int sourceHeight) {
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    public void computeSourceRect(RectF sourceRect,
            float x, float y,
            float width, float height,
            float overallWidth, float overallHeight) {

        float diffX, diffY;
        float xRatio, yRatio;

        // Try using source width as reference
        float scaledSourceHeight = overallWidth * sourceHeight / sourceWidth;
        if (scaledSourceHeight <= overallHeight) {
            diffX = 0f;
            diffY = -(overallHeight - scaledSourceHeight) / 2f / overallHeight;
            xRatio = 1f;
            yRatio = overallHeight / scaledSourceHeight;
        } else {
            float scaledSourceWidth = overallHeight * sourceWidth / sourceHeight;
            diffX = -(overallWidth - scaledSourceWidth) / 2f / overallWidth;
            diffY = 0f;
            xRatio = overallWidth / scaledSourceWidth;
            yRatio = 1f;
        }

        // Create the rectangle on the image
        sourceRect.set(
                sourceWidth * (x + diffX) * xRatio,
                sourceHeight * (y + diffY) * yRatio,
                sourceWidth * (x + diffX + width) * xRatio,
                sourceHeight * (y + diffY + height) * yRatio
        );
    }

    public SizeConverter translateCanvasAndGetConverter(Canvas canvas, float scale, float overallWidth, float overallHeight) {

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

        sizeConverter.scale = scale;
        sizeConverter.sourceRatioOnZoom1 = sourceRatioOnZoom1;
        return sizeConverter;
    }


    public static class SizeConverter {

        private float scale, sourceRatioOnZoom1;

        float convert(float pixelSize) {
            return pixelSize * sourceRatioOnZoom1 * scale;
        }
    }
}
