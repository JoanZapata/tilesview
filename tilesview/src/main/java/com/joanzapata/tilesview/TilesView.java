package com.joanzapata.tilesview;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.joanzapata.tilesview.internal.TilePool;
import com.joanzapata.tilesview.util.ScrollAndZoomDetector;

import java.util.ArrayList;
import java.util.List;

public class TilesView extends View implements ScrollAndZoomDetector.ScrollAndZoomListener {

    public static final int TILE_SIZE = 256;

    public static final boolean SHOW_DEBUG = true;

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

    private ScrollAndZoomDetector scrollAndZoomDetector;
    private RectF reusableRect = new RectF();
    private List<Layer> layers = new ArrayList<Layer>();

    public TilesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.tilePool = new TilePool();
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
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setStyle(Paint.Style.FILL);
    }

    public void addLayer(Layer layer) {
        layers.add(layer);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float contentWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float contentHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        canvas.save();
        canvas.translate(-offsetX, -offsetY);

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
        xIndexStart = Math.max(0, xIndexStart);
        yIndexStart = Math.max(0, yIndexStart);
        xIndexStop = (int) Math.min(Math.ceil(contentWidth * scale / TILE_SIZE) - 1, xIndexStop);
        yIndexStop = (int) Math.min(Math.ceil(contentHeight * scale / TILE_SIZE) - 1, yIndexStop);

        // Loop through tiles
        for (int xIndex = xIndexStart; xIndex <= xIndexStop; xIndex++) {
            for (int yIndex = yIndexStart; yIndex <= yIndexStop; yIndex++) {

                Bitmap tile = tilePool.getTile(zoomLevel, xIndex, yIndex, contentWidth, contentHeight);
                float left = xIndex * TILE_SIZE * zoomDiff;
                float top = yIndex * TILE_SIZE * zoomDiff;

                if (tile != null && !tile.isRecycled()) {
                    reusableRect.set((int) left, (int) top,
                            (int) (left + TILE_SIZE * zoomDiff),
                            (int) (top + TILE_SIZE * zoomDiff));
                    canvas.drawBitmap(tile, null, reusableRect, null);
                } else {
                    // Draw black rect as a placeholder
                    canvas.drawRect(left, top,
                            left + TILE_SIZE * zoomDiff,
                            top + TILE_SIZE * zoomDiff, backgroundPaint);
                }

                if (SHOW_DEBUG) {
                    canvas.drawRect(left, top,
                            left + TILE_SIZE * zoomDiff,
                            top + TILE_SIZE * zoomDiff, debugPaint);
                    canvas.drawText(xIndex + "," + yIndex,
                            left + TILE_SIZE * zoomDiff / 2,
                            top + TILE_SIZE * zoomDiff / 2 + debugPaint.getTextSize() / 4,
                            debugPaint);
                    canvas.drawText(zoomLevel + "",
                            left + TILE_SIZE * zoomDiff - 30,
                            top + debugPaint.getTextSize() + 5,
                            debugPaint);
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
    public boolean onScale(float scaleFactor) {
        this.scale *= scaleFactor;
        this.zoomLevel = Math.round(this.scale * 10f);
        invalidate();
        return true;
    }

    @Override
    public void onScaleEnd() {
        this.scale = this.zoomLevel / 10f;
        invalidate();
    }

}
