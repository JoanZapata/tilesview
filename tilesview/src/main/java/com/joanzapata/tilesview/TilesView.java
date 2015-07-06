package com.joanzapata.tilesview;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.joanzapata.tilesview.internal.TilePool;
import com.joanzapata.tilesview.util.ScrollAndZoomDetector;

import java.util.ArrayList;
import java.util.List;

public class TilesView extends View implements ScrollAndZoomDetector.ScrollAndZoomListener {

    public static final int TILE_SIZE = 256;

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
    private RectF reusableRect = new RectF();
    private List<Layer> layers = new ArrayList<Layer>();
    private boolean debug = false;

    public TilesView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (getBackground() instanceof ColorDrawable)
            backgroundColor = ((ColorDrawable) getBackground()).getColor();
        this.tilePool = new TilePool(backgroundColor);
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

    @Override
    protected void onDraw(Canvas canvas) {
        float contentWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float contentHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        canvas.save();
        canvas.translate((int) -offsetX, (int) -offsetY);

        // Find the top left index for the current scale and canvas size
        float zoomDiff = scale / (zoomLevel / 10f);
        float xOffsetOnContent = offsetX * zoomDiff;
        float yOffsetOnContent = offsetY * zoomDiff;
        int xIndexStart = (int) (xOffsetOnContent / TILE_SIZE);
        int yIndexStart = (int) (yOffsetOnContent / TILE_SIZE);
        int xIndexStop = (int) ((xOffsetOnContent + getWidth()) / TILE_SIZE);
        int yIndexStop = (int) ((yOffsetOnContent + getHeight()) / TILE_SIZE);

        // Adjustments for edge cases
        if (xOffsetOnContent < 0) xIndexStart--;
        if (yOffsetOnContent < 0) yIndexStart--;

        if (zoomDiff != 1) {
            // FIXME could be more accurate, should only expand required values to fill the screen
            xIndexStart--;
            yIndexStart--;
            xIndexStop++;
            yIndexStop++;
        }

        int xGridIndexStart = Math.max(0, xIndexStart);
        int yGridIndexStart = Math.max(0, yIndexStart);
        int xGridIndexStop = (int) Math.min(Math.ceil(contentWidth * scale / TILE_SIZE) - 1, xIndexStop);
        int yGridIndexStop = (int) Math.min(Math.ceil(contentHeight * scale / TILE_SIZE) - 1, yIndexStop);

        // Loop through all tiles visible on the screen
        for (int xIndex = xIndexStart; xIndex <= xIndexStop; xIndex++) {
            for (int yIndex = yIndexStart; yIndex <= yIndexStop; yIndex++) {

                // Compute the current tile position on canvas
                float spread = zoomDiff != 1f ? +1f : 0f;
                float left = xIndex * (float) TILE_SIZE * zoomDiff;
                float top = yIndex * (float) TILE_SIZE * zoomDiff;
                float right = left + TILE_SIZE * zoomDiff + spread;
                float bottom = top + TILE_SIZE * zoomDiff + spread;

                // If this tile is not outside the user content
                if (xIndex >= xGridIndexStart && xIndex <= xGridIndexStop &&
                        yIndex >= yGridIndexStart && yIndex <= yGridIndexStop) {

                    // Request the tile and draw it
                    Bitmap tile = tilePool.getTile(zoomLevel, xIndex, yIndex, contentWidth, contentHeight);
                    if (tile != null && !tile.isRecycled()) {
                        reusableRect.set(left, top, right, bottom);
                        canvas.drawBitmap(tile, null, reusableRect, backgroundPaint);
                    }

                    if (debug) {
                        int lineSize = 20;
                        canvas.drawLine(left, top, left + lineSize, top, debugPaint);
                        canvas.drawLine(left, top, left, top + lineSize, debugPaint);
                        canvas.drawLine(left, bottom - lineSize, left, bottom, debugPaint);
                        canvas.drawLine(left, bottom, left, bottom + lineSize, debugPaint);
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

    public void setTileRenderer(TileRenderer tileRenderer) {
        tilePool.setTileRenderer(tileRenderer);
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
        zoomLevel = Math.round(this.scale * 10f);
        invalidate();
        return true;
    }

    @Override
    public void onScaleEnd() {
        this.scale = this.zoomLevel / 10f;
        invalidate();
    }

}
