package com.joanzapata.tilesview.adapter;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import com.joanzapata.tilesview.AnimationCallback;
import com.joanzapata.tilesview.TilesView;
import com.joanzapata.tilesview.TilesViewAdapter;

public abstract class FixedSizeAdapter implements TilesViewAdapter {

    private final ThreadLocal<RectF> sourceRectTL, destRectTL;
    private final float sourceWidth;
    private final float sourceHeight;
    private float scale;
    private float sourceInitialRatio;
    private TilesView tilesView;

    private int minZoomLevel = DEFAULT_MIN_ZOOM_LEVEL;
    private int maxZoomLevel = DEFAULT_MAX_ZOOM_LEVEL;

    public FixedSizeAdapter(float width, float height) {
        this.sourceWidth = width;
        this.sourceHeight = height;
        sourceRectTL = new ThreadLocal<RectF>();
        destRectTL = new ThreadLocal<RectF>();
    }

    @Override
    public void attachTilesView(TilesView tilesView) {
        this.tilesView = tilesView;
    }

    @Override
    public int getMinZoomLevel() {
        return minZoomLevel;
    }

    @Override
    public int getMaxZoomLevel() {
        return maxZoomLevel;
    }

    @Override
    public void getBounds(RectF bounds) {
        CenterCropTranslator translator = CenterCropTranslator.get(tilesView, sourceWidth, sourceHeight);
        bounds.set(
                translator.sourceToContentX(0) * translator.currentContentScale,
                translator.sourceToContentY(0) * translator.currentContentScale,
                translator.sourceToContentX(sourceWidth) * translator.currentContentScale,
                translator.sourceToContentY(sourceHeight) * translator.currentContentScale);
    }

    @Override
    public boolean isThreadSafe() {
        // Default value, can be overridden
        return false;
    }

    @Override
    public void drawTile(Canvas canvas,
            float xRatio, float yRatio,
            float widthRatio, float heightRatio,
            float contentInitialWidth, float contentInitialHeight,
            float scale) {

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

        // Project the target tile on the user image
        CenterCropTranslator translator = CenterCropTranslator.get(tilesView, sourceWidth, sourceHeight);
        sourceRect.set(
                translator.contentToSourceX(xRatio),
                translator.contentToSourceY(yRatio),
                translator.contentToSourceX(xRatio + widthRatio),
                translator.contentToSourceY(yRatio + heightRatio));

        // If out of the user image, ignore this tile
        if (sourceRect.right <= 0 ||
                sourceRect.left >= sourceWidth ||
                sourceRect.bottom <= 0 ||
                sourceRect.top >= sourceHeight) {
            return;
        }

        // Will probably draw the whole tile...
        destRect.set(0, 0, canvas.getWidth(), canvas.getHeight());

        // ... but at the edge of the user image it's possible that
        // a tile contains some empty space. Removes that space
        // for performance.
        if (sourceRect.top < 0) {
            destRect.top -= sourceRect.top * translator.initialContentScale * scale;
            sourceRect.top = 0;
        }
        if (sourceRect.left < 0) {
            destRect.left -= sourceRect.left * translator.initialContentScale * scale;
            sourceRect.left = 0;
        }
        if (sourceRect.right > sourceWidth) {
            destRect.right += (sourceWidth - sourceRect.right) * translator.initialContentScale * scale;
            sourceRect.right = sourceWidth;
        }
        if (sourceRect.bottom > sourceHeight) {
            destRect.bottom += (sourceHeight - sourceRect.bottom) * translator.initialContentScale * scale;
            sourceRect.bottom = sourceHeight;
        }

        // Call user code
        drawTile(canvas, sourceRect, destRect);

    }

    @Override
    public final void onClick(float xRatio, float yRatio, float contentInitialWidth, float contentInitialHeight, float scale) {
        CenterCropTranslator translator = CenterCropTranslator.get(tilesView, sourceWidth, sourceHeight);
        float contentX = translator.contentToSourceX(xRatio);
        float contentY = translator.contentToSourceY(yRatio);
        onClick(contentX, contentY, translator.currentSourceScale);
    }

    @Override
    public void getPosition(float x, float y, PointF position) {
        CenterCropTranslator translator = CenterCropTranslator.get(tilesView, sourceWidth, sourceHeight);
        tilesView.getPositionInView(
                translator.tilesViewToSourceX(x),
                translator.tilesViewToSourceY(y),
                position);
    }

    @Override
    public final void drawLayer(Canvas canvas, float scale, float contentInitialWidth, float contentInitialHeight) {
        CenterCropTranslator translator = CenterCropTranslator.get(tilesView, sourceWidth, sourceHeight);
        canvas.translate(
                translator.sourceToContentX(0) * translator.currentContentScale,
                translator.sourceToContentY(0) * translator.currentContentScale);
        this.scale = scale;
        this.sourceInitialRatio = translator.initialContentScale;
        drawLayer(canvas, scale);
    }

    @Override
    public void animateTo(float x, float y, int zoomLevel, AnimationCallback callback) {
        CenterCropTranslator translator = CenterCropTranslator.get(tilesView, sourceWidth, sourceHeight);
        float xT = translator.sourceToContentX(x);
        float yT = translator.sourceToContentY(y);
        tilesView.animateTo(xT, yT, zoomLevel, callback);
    }

    @Override
    public void animateTo(float x, float y, int zoomLevel) {
        this.animateTo(x, y, zoomLevel, null);
    }

    @Override
    public void animateTo(float x, float y, AnimationCallback callback) {
        this.animateTo(x, y, tilesView.getZoomLevel(), callback);
    }

    @Override
    public void animateTo(float x, float y) {
        this.animateTo(x, y, tilesView.getZoomLevel(), null);
    }

    /**
     * Animate to a position and zoom that contains the specified bounds.
     * @param left              The left offset of the bounds.
     * @param top               The top offset of the bounds.
     * @param right             The right offset of the bounds.
     * @param bottom            The bottom offset of the bounds.
     * @param animationCallback The callback that will be called when the animation ends.
     */
    public void animateTo(float left, float top, float right, float bottom, AnimationCallback animationCallback) {
        CenterCropTranslator translator = CenterCropTranslator.get(tilesView, sourceWidth, sourceHeight);
        float targetContentWidth = (right - left) * translator.initialContentScale;
        float targetContentHeight = (bottom - top) * translator.initialContentScale;
        float maxScaleX = tilesView.getContentWidth() / targetContentWidth;
        float maxScaleY = tilesView.getContentHeight() / targetContentHeight;
        float scale = Math.min(maxScaleX, maxScaleY);
        int zoomLevel = (int) (scale < 1 ? scale + 10
                : Math.log(scale * 10 - 10) / Math.log(2) + 10);
        if (zoomLevel > tilesView.getZoomLevel()) {
            zoomLevel = tilesView.getZoomLevel();
        }
        animateTo((left + right) / 2f, (top + bottom) / 2f, zoomLevel, animationCallback);
    }

    /**
     * Render a tile.
     * @param canvas     The canvas on which to draw the tile.
     * @param sourceRect The bounds of the tile in the source image, in pixels.
     * @param destRect   The bounds on which to draw the destination image, in pixels.
     */
    protected abstract void drawTile(Canvas canvas, RectF sourceRect, RectF destRect);

    public void drawLayer(Canvas canvas, float scale) {
        // Default implementation does nothing
    }

    public void onClick(float x, float y, float scale) {
        // Default implementation does nothing
    }

    public void setMinZoomLevel(int minZoomLevel) {
        this.minZoomLevel = minZoomLevel;
    }

    public void setMaxZoomLevel(int maxZoomLevel) {
        this.maxZoomLevel = maxZoomLevel;
    }

    /**
     * TODO JAVADOC
     * @param pixelSizeOnSourceImage
     * @return
     */
    protected float scaled(float pixelSizeOnSourceImage) {
        return pixelSizeOnSourceImage * sourceInitialRatio * scale;
    }

    public final void invalidate(float l, float t, float r, float b) {
        CenterCropTranslator translator = CenterCropTranslator.get(tilesView, sourceWidth, sourceHeight);
        tilesView.invalidateTiles(
                translator.sourceToContentX(l),
                translator.sourceToContentY(t),
                translator.sourceToContentX(r),
                translator.sourceToContentY(b));
    }

    private static class CenterCropTranslator {

        private final static ThreadLocal<CenterCropTranslator> translatorTL = new ThreadLocal<CenterCropTranslator>();
        public float xDiff, yDiff;
        public float xFactor, yFactor;
        public float initialContentScale;
        public float currentSourceScale, currentContentScale;
        public float contentWidth, contentHeight;
        public float sourceWidth, sourceHeight;

        public static CenterCropTranslator get(TilesView tilesView, float sourceWidth, float sourceHeight) {
            CenterCropTranslator translator = translatorTL.get();
            if (translator == null) {
                translator = new CenterCropTranslator();
                translatorTL.set(translator);
            }
            translator.update(tilesView, sourceWidth, sourceHeight);
            return translator;
        }

        private void update(TilesView tilesView, float sourceWidth, float sourceHeight) {
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
            contentWidth = tilesView.getContentWidth();
            contentHeight = tilesView.getContentHeight();
            float scaledSourceHeight = tilesView.getContentWidth() * sourceHeight / sourceWidth;
            if (scaledSourceHeight <= contentHeight) {
                initialContentScale = contentWidth / sourceWidth;
                xDiff = 0f;
                yDiff = -(contentHeight - scaledSourceHeight) / 2f / contentHeight;
                xFactor = 1f;
                yFactor = contentHeight / scaledSourceHeight;
            } else {
                initialContentScale = contentHeight / sourceHeight;
                float scaledSourceWidth = contentHeight * sourceWidth / sourceHeight;
                xDiff = -(contentWidth - scaledSourceWidth) / 2f / contentWidth;
                yDiff = 0f;
                xFactor = contentWidth / scaledSourceWidth;
                yFactor = 1f;
            }
            currentContentScale = tilesView.getScale();
            currentSourceScale = initialContentScale * currentContentScale;
        }

        public float tilesViewToSourceX(float x) {
            return (x / (xFactor * sourceWidth) - xDiff) * contentWidth;
        }

        public float tilesViewToSourceY(float y) {
            return (y / (yFactor * sourceHeight) - yDiff) * contentHeight;
        }

        public float contentToSourceX(float xRatio) {
            return (xRatio + xDiff) * xFactor * sourceWidth;
        }

        public float contentToSourceY(float yRatio) {
            return (yRatio + yDiff) * yFactor * sourceHeight;
        }

        public float sourceToContentX(float x) {
            return -xDiff * contentWidth + x * initialContentScale;
        }

        public float sourceToContentY(float y) {
            return -yDiff * contentHeight + y * initialContentScale;
        }
    }
}
