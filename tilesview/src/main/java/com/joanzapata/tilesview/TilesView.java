package com.joanzapata.tilesview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.joanzapata.tilesview.internal.Tile;
import com.joanzapata.tilesview.internal.TilePool;
import com.joanzapata.tilesview.util.ScrollAndZoomDetector;

import java.util.ArrayList;
import java.util.List;

public class TilesView extends View implements ScrollAndZoomDetector.ScrollAndZoomListener, TilePool.TilePoolListener {

    public static final int TILE_SIZE = 256;

    private static final int MAX_ZOOM_LEVEL = 300;

    /** Initial scale is 1, scale can't be < 1 */
    private float scale;

    /** X offset of the top left corner of the screen in the global image */
    private float offsetX;

    /** Y offset of the top left corner of the screen in the global image */
    private float offsetY;

    /** Zoom level is scale * 10 rounded to the nearest integer (e.g. 12 for x1,23) */
    private int zoomLevel;

    /** Retains all tiles in memory */
    private TilePool tilePool;

    private Paint debugPaint;
    private Paint backgroundPaint;

    private int backgroundColor;

    private ScrollAndZoomDetector scrollAndZoomDetector;
    private RectF reusableRectF = new RectF();
    private Rect reusableRect = new Rect();
    private List<Layer> layers = new ArrayList<Layer>();
    private boolean debug = false;
    private int doubleTapZoomLevelDiff = 4;

    public TilesView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.backgroundColor = Color.BLACK;
        if (getBackground() instanceof ColorDrawable)
            this.backgroundColor = ((ColorDrawable) getBackground()).getColor();
        this.tilePool = new TilePool(backgroundColor, this);
        this.scale = 1;
        this.zoomLevel = (int) (this.scale * 10);
        this.offsetX = -getPaddingLeft();
        this.offsetY = -getPaddingTop();
        this.scrollAndZoomDetector = new ScrollAndZoomDetector(context, this, this);

        debugPaint = new Paint();
        debugPaint.setAntiAlias(true);
        debugPaint.setColor(Color.GRAY);
        debugPaint.setTextSize(40);
        debugPaint.setTextAlign(Paint.Align.CENTER);
        debugPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint = new Paint();
        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setStyle(Paint.Style.FILL);
        setBackground(null);
    }

    public void addLayer(Layer layer) {
        layers.add(layer);
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        invalidate();
    }

    public void setDoubleTapZoomLevelDiff(int doubleTapZoomLevelDiff) {
        this.doubleTapZoomLevelDiff = doubleTapZoomLevelDiff;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float contentWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float contentHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        canvas.save();
        canvas.translate((int) -offsetX, (int) -offsetY);

        // Retrieve a placeholder for tiles not yet rendered
        Bitmap placeholder = tilePool.getPlaceholder(contentWidth, contentHeight);
        float placeholderRatio = 0f;
        if (placeholder != null) {
            placeholderRatio = contentWidth / placeholder.getWidth();
        }

        // Find the top left index for the current scale and canvas size
        float zoomDiff = scale / (zoomLevel / 10f);
        float xOffsetOnContent = offsetX / scale;
        float yOffsetOnContent = offsetY / scale;
        float screenWidthOnContent = getWidth() / scale;
        float screenHeightOnContent = getHeight() / scale;
        float tileSizeOnContent = TILE_SIZE / (zoomLevel / 10f);
        int xIndexStart = (int) (xOffsetOnContent / tileSizeOnContent);
        int yIndexStart = (int) (yOffsetOnContent / tileSizeOnContent);
        int xIndexStop = (int) ((xOffsetOnContent + screenWidthOnContent) / tileSizeOnContent);
        int yIndexStop = (int) ((yOffsetOnContent + screenHeightOnContent) / tileSizeOnContent);

        // Adjustments for edge cases
        if (xOffsetOnContent < 0) xIndexStart--;
        if (yOffsetOnContent < 0) yIndexStart--;

        int xGridIndexStart = Math.max(0, xIndexStart);
        int yGridIndexStart = Math.max(0, yIndexStart);
        int xGridIndexStop = (int) Math.min(Math.ceil(contentWidth * scale / TILE_SIZE) - 1, xIndexStop);
        int yGridIndexStop = (int) Math.min(Math.ceil(contentHeight * scale / TILE_SIZE) - 1, yIndexStop);

        /*
         * Loop through the 2D grid. This loop is a little complex
         * because it starts at the top left corner and reach the
         * center in a spiral like movement. See https://goo.gl/r7As7V
         */
        int xIndex = xIndexStart;
        int yIndex = yIndexStart;
        while (xIndex <= xIndexStop || yIndex <= yIndexStop) {

            while (xIndex <= xIndexStop && xIndex >= xIndexStart) {

                drawTile(xIndex, yIndex, canvas,
                        placeholder, zoomDiff, placeholderRatio,
                        xGridIndexStart, xGridIndexStop,
                        yGridIndexStart, yGridIndexStop,
                        contentWidth, contentHeight);

                xIndex++;
            }
            xIndex = xIndexStop;
            yIndex++;
            yIndexStart++;
            if (xIndexStart > xIndexStop || yIndexStart > yIndexStop) break;

            while (yIndex <= yIndexStop && yIndex >= yIndexStart) {

                drawTile(xIndex, yIndex, canvas,
                        placeholder, zoomDiff, placeholderRatio,
                        xGridIndexStart, xGridIndexStop,
                        yGridIndexStart, yGridIndexStop,
                        contentWidth, contentHeight);

                yIndex++;
            }
            yIndex = yIndexStop;
            xIndexStop--;
            xIndex--;
            if (xIndexStart > xIndexStop || yIndexStart > yIndexStop) break;

            while (xIndex <= xIndexStop && xIndex >= xIndexStart) {

                drawTile(xIndex, yIndex, canvas,
                        placeholder, zoomDiff, placeholderRatio,
                        xGridIndexStart, xGridIndexStop,
                        yGridIndexStart, yGridIndexStop,
                        contentWidth, contentHeight);

                xIndex--;
            }
            xIndex = xIndexStart;
            yIndex--;
            yIndexStop--;
            if (xIndexStart > xIndexStop || yIndexStart > yIndexStop) break;

            while (yIndex <= yIndexStop && yIndex >= yIndexStart) {

                drawTile(xIndex, yIndex, canvas,
                        placeholder, zoomDiff, placeholderRatio,
                        xGridIndexStart, xGridIndexStop,
                        yGridIndexStart, yGridIndexStop,
                        contentWidth, contentHeight);

                yIndex--;
            }
            yIndex = yIndexStart;
            xIndexStart++;
            xIndex++;
            if (xIndexStart > xIndexStop || yIndexStart > yIndexStop) break;

        }

        // Render user layers
        for (int i = 0, size = layers.size(); i < size; i++) {
            Layer layer = layers.get(i);
            canvas.save();
            layer.renderLayer(canvas, scale, contentWidth, contentHeight);
            canvas.restore();
        }

        canvas.restore();

    }

    private void drawTile(
            int xIndex, int yIndex,
            Canvas canvas, Bitmap placeholder,
            float zoomDiff, float placeholderRatio,
            int xGridIndexStart, int xGridIndexStop,
            int yGridIndexStart, int yGridIndexStop,
            float contentWidth, float contentHeight) {

        // Compute the current tile position on canvas
        float spread = zoomDiff != 1f ? +1f : 0f;
        float left = xIndex * (float) TILE_SIZE * zoomDiff;
        float top = yIndex * (float) TILE_SIZE * zoomDiff;
        float right = left + TILE_SIZE * zoomDiff + spread;
        float bottom = top + TILE_SIZE * zoomDiff + spread;

        // If this tile is not outside the user content
        if (xIndex >= xGridIndexStart && xIndex <= xGridIndexStop &&
                yIndex >= yGridIndexStart && yIndex <= yGridIndexStop) {

            // Request the tile
            Bitmap tile = tilePool.getTile(zoomLevel, xIndex, yIndex, contentWidth, contentHeight);

            if (tile != null && !tile.isRecycled()) {
                // Draw the tile if any
                reusableRectF.set(left, top, right, bottom);
                canvas.drawBitmap(tile, null, reusableRectF, backgroundPaint);

            } else if (placeholder != null && xIndex >= 0 && yIndex >= 0) {
                // Draw the placeholder if any
                reusableRectF.set(left, top, right, bottom);
                float placeholderTileSize = TILE_SIZE / placeholderRatio / scale * zoomDiff;
                reusableRect.set(
                        (int) (xIndex * placeholderTileSize),
                        (int) (yIndex * placeholderTileSize),
                        (int) ((xIndex + 1f) * placeholderTileSize),
                        (int) ((yIndex + 1f) * placeholderTileSize));

                if (reusableRect.right > placeholder.getWidth()) {
                    float rightOffsetOnPlaceholderTile = reusableRect.right - placeholder.getWidth();
                    float rightOffset = rightOffsetOnPlaceholderTile * (TILE_SIZE * zoomDiff) / placeholderTileSize;
                    canvas.drawRect(
                            reusableRectF.right - rightOffset - 1, reusableRectF.top,
                            reusableRectF.right, reusableRectF.bottom,
                            backgroundPaint);
                    reusableRectF.right -= rightOffset;
                    reusableRect.right = placeholder.getWidth();
                }

                if (reusableRect.bottom > placeholder.getHeight()) {
                    float bottomOffsetOnPlaceholderTile = reusableRect.bottom - placeholder.getHeight();
                    float bottomOffset = bottomOffsetOnPlaceholderTile * (TILE_SIZE * zoomDiff) / placeholderTileSize;
                    canvas.drawRect(
                            reusableRectF.left, reusableRectF.bottom - bottomOffset - 1,
                            reusableRectF.right, reusableRectF.bottom,
                            backgroundPaint);
                    reusableRectF.bottom -= bottomOffset;
                    reusableRect.bottom = placeholder.getHeight();
                }

                canvas.drawBitmap(placeholder, reusableRect, reusableRectF, null);


            } else {
                // Draw the background otherwise
                canvas.drawRect(left, top, right, bottom, backgroundPaint);
            }

            if (debug) {
                int lineSize = 20;
                canvas.drawLine(left, top, left + lineSize, top, debugPaint);
                canvas.drawLine(left, top, left, top + lineSize, debugPaint);
                canvas.drawLine(left, bottom - lineSize, left, bottom, debugPaint);
                canvas.drawLine(left, bottom, left + lineSize, bottom, debugPaint);
                canvas.drawLine(right - lineSize, top, right, top, debugPaint);
                canvas.drawLine(right, top, right, top + lineSize, debugPaint);
                canvas.drawLine(right - lineSize, bottom, right, bottom, debugPaint);
                canvas.drawLine(right, bottom - lineSize, right, bottom, debugPaint);
                canvas.drawText(xIndex + "," + yIndex,
                        (left + right) / 2f,
                        (top + bottom) / 2f + debugPaint.getTextSize() / 4,
                        debugPaint);
                canvas.drawText(zoomLevel + "",
                        right - 30,
                        top + debugPaint.getTextSize() + 5,
                        debugPaint);
            }

        } else {

            // If the current tile is outside user content, draw placeholder
            canvas.drawRect(left, top, right, bottom, backgroundPaint);

        }
    }

    public void setTileRenderer(TileRenderer tileRenderer) {
        setTileRenderer(tileRenderer, true);
    }

    public void setTileRenderer(TileRenderer tileRenderer, boolean threadSafe) {
        tilePool.setTileRenderer(tileRenderer, threadSafe);
        postInvalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        double tilesOnWidth = Math.ceil(w / (TILE_SIZE * 0.9f)) + 1;
        double tilesOnHeight = Math.ceil(h / (TILE_SIZE * 0.9f)) + 1;
        int maxTilesOnScreen = (int) (tilesOnWidth * tilesOnHeight);
        tilePool.setMaxTasks(maxTilesOnScreen);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return scrollAndZoomDetector.onTouchEvent(event);
    }

    @Override
    public boolean onScroll(float distanceX, float distanceY) {
        offsetX += distanceX;
        offsetY += distanceY;
        invalidate();
        return true;
    }

    @Override
    public boolean onScale(float scaleFactor, float focusX, float focusY) {

        // Move offsets so that the focus point remains the same
        float newScale = scale * scaleFactor;
        float contentWidthBefore = getWidth() * scale;
        float contentWidthAfter = getWidth() * newScale;
        float contentFocusXBefore = offsetX + focusX;
        float contentFocusXBeforeRatio = contentFocusXBefore / contentWidthBefore;
        float contentFocusXAfter = contentFocusXBeforeRatio * contentWidthAfter;
        offsetX += contentFocusXAfter - contentFocusXBefore;

        float contentHeightBefore = getHeight() * scale;
        float contentHeightAfter = getHeight() * newScale;
        float contentFocusYBefore = offsetY + focusY;
        float contentFocusYBeforeRatio = contentFocusYBefore / contentHeightBefore;
        float contentFocusYAfter = contentFocusYBeforeRatio * contentHeightAfter;
        offsetY += contentFocusYAfter - contentFocusYBefore;

        scale = newScale;
        zoomLevel = Math.min(MAX_ZOOM_LEVEL, Math.round(scale * 10f));
        invalidate();
        return true;
    }

    @Override
    public boolean onDoubleTap(final float focusX, final float focusY) {
        final ValueAnimator valueAnimator = ValueAnimator.ofFloat(scale, (zoomLevel + doubleTapZoomLevelDiff) / 10f);
        valueAnimator.setDuration(1000);
        valueAnimator.start();

        Runnable animation = new Runnable() {
            @Override
            public void run() {
                Float animatedValue = (Float) valueAnimator.getAnimatedValue();
                onScale(animatedValue / scale, focusX, focusY);
                invalidate();

                if (valueAnimator.isRunning()) {
                    postOnAnimation(this);
                } else {
                    onScaleEnd();
                }

            }
        };

        postOnAnimation(animation);
        return true;
    }

    @Override
    public void onScaleEnd() {
        this.scale = Math.round(this.scale * 10f) / 10f;
        invalidate();
    }

    @Override
    public void onTileRendered(Tile tile) {
        postInvalidate();
    }
}
