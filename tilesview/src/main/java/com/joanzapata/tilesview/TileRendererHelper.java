package com.joanzapata.tilesview;

import android.graphics.RectF;

public class TileRendererHelper {

    private final int sourceWidth, sourceHeight;

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
}
