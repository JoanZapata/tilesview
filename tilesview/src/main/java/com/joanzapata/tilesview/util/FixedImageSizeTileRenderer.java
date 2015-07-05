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
    public final void renderTile(Canvas canvas,
            float xRatio, float yRatio,
            float widthRatio, float heightRatio,
            float contentInitialWidth, float contentInitialHeight) {

        float xDiff, yDiff;
        float xFactor, yFactor;

        // Try using source width as reference
        float scaledSourceHeight = contentInitialWidth * sourceHeight / sourceWidth;
        if (scaledSourceHeight <= contentInitialHeight) {
            xDiff = 0f;
            yDiff = -(contentInitialHeight - scaledSourceHeight) / 2f / contentInitialHeight;
            xFactor = 1f;
            yFactor = contentInitialHeight / scaledSourceHeight;
        } else {
            float scaledSourceWidth = contentInitialHeight * sourceWidth / sourceHeight;
            xDiff = -(contentInitialWidth - scaledSourceWidth) / 2f / contentInitialWidth;
            yDiff = 0f;
            xFactor = contentInitialWidth / scaledSourceWidth;
            yFactor = 1f;
        }

        // Create the rectangle on the image
        sourceRect.set(
                sourceWidth * (xRatio + xDiff) * xFactor,
                sourceHeight * (yRatio + yDiff) * yFactor,
                sourceWidth * (xRatio + xDiff + widthRatio) * xFactor,
                sourceHeight * (yRatio + yDiff + heightRatio) * yFactor
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
