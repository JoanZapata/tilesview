package com.joanzapata.tilesview.adapter;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import com.joanzapata.tilesview.CancelableCallback;
import com.joanzapata.tilesview.TilesView;
import com.joanzapata.tilesview.TilesViewAdapter;

public abstract class FixedSizeAdapter implements TilesViewAdapter {

    private final ThreadLocal<RectF> sourceRectTL, destRectTL;
    private final float sourceWidth;
    private final float sourceHeight;

    public FixedSizeAdapter(float width, float height) {
        this.sourceWidth = width;
        this.sourceHeight = height;
        sourceRectTL = new ThreadLocal<RectF>();
        destRectTL = new ThreadLocal<RectF>();
    }

    @Override
    public void attachTilesView(TilesView tilesView) {

    }

    @Override
    public int getMinZoomLevel() {
        return 0;
    }

    @Override
    public int getMaxZoomLevel() {
        return 0;
    }

    @Override
    public int getOverscrollLeft() {
        return 0;
    }

    @Override
    public int getOverscrollRight() {
        return 0;
    }

    @Override
    public int getOverscrollTop() {
        return 0;
    }

    @Override
    public int getOverscrollBottom() {
        return 0;
    }

    @Override
    public boolean isThreadSafe() {
        return false;
    }

    @Override
    public void drawTile(Canvas canvas,
            float xRatio, float yRatio,
            float widthRatio, float heightRatio,
            float contentInitialWidth, float contentInitialHeight,
            float scale) {

        float xDiff, yDiff;
        float xFactor, yFactor;
        float initialScale;

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

        // Try using source sourceWidth as reference
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

        // Project the target tile on the user image
        sourceRect.set(
                sourceWidth * (xRatio + xDiff) * xFactor,
                sourceHeight * (yRatio + yDiff) * yFactor,
                sourceWidth * (xRatio + xDiff + widthRatio) * xFactor,
                sourceHeight * (yRatio + yDiff + heightRatio) * yFactor
        );

        // If out of the user image, ignore this tile
        if (sourceRect.right <= 0 ||
                sourceRect.left >= sourceWidth ||
                sourceRect.bottom <= 0 ||
                sourceRect.top >= sourceHeight) {
            return;
        }

        // Will probably draw the whole tile
        destRect.set(0, 0, canvas.getWidth(), canvas.getHeight());

        // But at the edge of the user image it's possible that
        // a tile contains some empty space. Removes that space
        // for performance.
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

    @Override
    public void onClick(float xRatio, float yRatio, float contentInitialWidth, float contentInitialHeight, float scale) {

    }

    @Override
    public PointF getPosition(float x, float y) {
        return null;
    }

    @Override
    public void drawLayer(Canvas canvas, float scale, float contentInitialWidth, float contentInitialHeight) {

    }

    @Override
    public void animateTo(float x, float y, int zoomLevel, CancelableCallback callback) {

    }

    @Override
    public void animateTo(float x, float y, int zoomLevel) {

    }

    /**
     * Render a tile.
     * @param canvas     The canvas on which to draw the tile.
     * @param sourceRect The bounds of the tile in the source image, in pixels.
     * @param destRect   The bounds on which to draw the destination image, in pixels.
     */
    protected abstract void renderTile(Canvas canvas, RectF sourceRect, RectF destRect);

}
