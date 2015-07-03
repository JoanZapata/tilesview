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

    /** Initial zoom is 1, zoom can't be < 1 */
    private float zoom;

    /** X offset of the top left corner of the screen in the global image */
    private float offsetX;

    /** Y offset of the top left corner of the screen in the global image */
    private float offsetY;

    /** Zoom level is zoom * 10 rounded to the nearest integer (e.g. 12 for x1,23) */
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
        this.zoom = 1;
        this.zoomLevel = (int) (this.zoom * 10);
        this.offsetX = 0;
        this.offsetY = 0;
        this.scrollAndZoomDetector = new ScrollAndZoomDetector(context, this);
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

        canvas.save();
        canvas.translate(-offsetX, -offsetY);

        // Find the top left index for the current zoom and canvas size
        float zoomDiff = zoom / (zoomLevel / 10f);
        float offsetXOnImage = offsetX * zoomDiff;
        float offsetYOnImage = offsetY * zoomDiff;
        int xStart = (int) (offsetXOnImage / TILE_SIZE);
        int yStart = (int) (offsetYOnImage / TILE_SIZE);
        int xStop = (int) ((offsetXOnImage + getWidth()) / TILE_SIZE);
        int yStop = (int) ((offsetYOnImage + getHeight()) / TILE_SIZE);

        // Adjustments for edge cases
        if (offsetXOnImage < 0) xStart--;
        if (offsetYOnImage < 0) yStart--;

        // Loop through tiles
        for (int x = xStart; x <= xStop; x++) {
            for (int y = yStart; y <= yStop; y++) {

                Bitmap tile = tilePool.getTile(zoomLevel, x, y, getWidth(), getHeight());
                float left = x * TILE_SIZE * zoomDiff;
                float top = y * TILE_SIZE * zoomDiff;

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
                    canvas.drawText(x + "," + y,
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
            layer.renderLayer(canvas, zoom, getWidth(), getHeight());
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
        this.zoom *= scaleFactor;
        this.zoomLevel = Math.round(this.zoom * 10f);
        invalidate();
        return true;
    }

    @Override
    public void onScaleEnd() {
        this.zoom = this.zoomLevel / 10f;
        invalidate();
    }

    public interface TileRenderer {
        void renderTile(Canvas canvas,
                float x, float y,
                float width, float height,
                float overallWidth, float overallHeight);
    }

    public interface Layer {
        void renderLayer(Canvas canvas, float scale,
                float overallWidth, float overallHeight);
    }

}
