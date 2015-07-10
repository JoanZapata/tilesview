package com.joanzapata.tilesview.util;

import android.graphics.Canvas;
import android.graphics.RectF;
import com.joanzapata.tilesview.TileRenderer;

public abstract class FixedImageSizeTileRenderer implements TileRenderer {

    private final float sourceWidth, sourceHeight;

    private final ThreadLocal<RectF> sourceRectTL, destRectTL;

    public FixedImageSizeTileRenderer(float sourceWidth, float sourceHeight) {
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
        this.sourceRectTL = new ThreadLocal<RectF>();
        this.destRectTL = new ThreadLocal<RectF>();
    }

    @Override
    public final void renderTile(Canvas canvas,
            float xRatio, float yRatio,
            float widthRatio, float heightRatio,
            float contentInitialWidth, float contentInitialHeight) {

        float xDiff, yDiff;
        float xFactor, yFactor;
        float initialScale;
        float scale = canvas.getWidth() / (widthRatio * contentInitialWidth);

        RectF sourceRect = sourceRectTL.get();
        if (sourceRect == null) {
            sourceRect = new RectF();
            sourceRectTL.set(sourceRect);
        }
        RectF destRect = destRectTL.get();
        if (destRect == null) {
            destRect = new RectF();
            destRectTL.set(destRect);
        }

        // Try using source width as reference
        float scaledSourceHeight = contentInitialWidth * sourceHeight / sourceWidth;
        if (scaledSourceHeight <= contentInitialHeight) {
            initialScale = contentInitialWidth / sourceWidth;
            xDiff = 0f;
            yDiff = -(contentInitialHeight - scaledSourceHeight) / 2f / contentInitialHeight;
            xFactor = 1f;
            yFactor = contentInitialHeight / scaledSourceHeight;
        } else {
            initialScale = contentInitialHeight / sourceHeight;
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

        destRect.set(0, 0, canvas.getWidth(), canvas.getHeight());

        if (sourceRect.top < 0) {
            destRect.top -= sourceRect.top * initialScale * scale;
            sourceRect.top = 0;
        }
        if (sourceRect.left < 0) {
            destRect.left -= sourceRect.left * initialScale * scale;
            sourceRect.left = 0;
        }
        if (sourceRect.right > sourceWidth) {
            destRect.right += (sourceWidth - sourceRect.right) * initialScale * scale;
            sourceRect.right = sourceWidth;
        }
        if (sourceRect.bottom > sourceHeight) {
            destRect.bottom += (sourceHeight - sourceRect.bottom) * initialScale * scale;
            sourceRect.bottom = sourceHeight;
        }

        // Call user code
        renderTile(canvas, sourceRect, destRect);

    }

    protected abstract void renderTile(Canvas canvas, RectF sourceRect, RectF destRect);


}
