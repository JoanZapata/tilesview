package com.joanzapata.tilesview.util;

import android.graphics.Canvas;
import android.graphics.RectF;
import com.joanzapata.tilesview.TileRenderer;

public abstract class FixedImageSizeTileRenderer implements TileRenderer {

    private final float sourceWidth, sourceHeight;
    private final RectF sourceRect;

    public FixedImageSizeTileRenderer(float sourceWidth, float sourceHeight) {
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
        this.sourceRect = new RectF();
    }

    @Override
    public final void renderTile(Canvas canvas, float x, float y, float width, float height, float overallWidth, float overallHeight) {

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

        if (sourceRect.right <= 0 ||
                sourceRect.left >= sourceWidth ||
                sourceRect.bottom <= 0 ||
                sourceRect.top >= sourceHeight) {
            return;
        }

        // Call user code
        renderTile(canvas, sourceRect);

    }

    protected abstract void renderTile(Canvas canvas, RectF sourceRect);


}
