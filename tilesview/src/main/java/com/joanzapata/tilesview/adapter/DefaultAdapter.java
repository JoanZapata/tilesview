package com.joanzapata.tilesview.adapter;

import android.graphics.Canvas;
import android.graphics.PointF;
import com.joanzapata.tilesview.CancelableCallback;
import com.joanzapata.tilesview.TilesView;
import com.joanzapata.tilesview.TilesViewAdapter;

public abstract class DefaultAdapter implements TilesViewAdapter {

    private TilesView tilesView;

    @Override
    public void attachTilesView(TilesView tilesView) {
        this.tilesView = tilesView;
    }

    @Override
    public int getMinZoomLevel() {
        // Default value, can be overridden
        return DEFAULT_MIN_ZOOM_LEVEL;
    }

    @Override
    public int getMaxZoomLevel() {
        // Default value, can be overridden
        return DEFAULT_MAX_ZOOM_LEVEL;
    }

    @Override
    public int getOverscrollLeft() {
        // Default value, can be overridden
        return 0;
    }

    @Override
    public int getOverscrollRight() {
        // Default value, can be overridden
        return 0;
    }

    @Override
    public int getOverscrollTop() {
        // Default value, can be overridden
        return 0;
    }

    @Override
    public int getOverscrollBottom() {
        // Default value, can be overridden
        return 0;
    }

    @Override
    public boolean isThreadSafe() {
        // Default value, can be overridden
        return false;
    }

    @Override
    public abstract void drawTile(Canvas canvas, float xRatio, float yRatio, float widthRatio, float heightRatio, float contentInitialWidth, float contentInitialHeight, float scale);

    @Override
    public void onClick(float xRatio, float yRatio, float contentInitialWidth, float contentInitialHeight, float scale) {
        // Default do nothing.
    }

    @Override
    public final PointF getPosition(float x, float y) {
        return tilesView.getPositionInView(x, y);
    }

    @Override
    public void drawLayer(Canvas canvas, float scale, float contentInitialWidth, float contentInitialHeight) {
        // Default do nothing
    }

    @Override
    public final void animateTo(float x, float y, int zoomLevel, CancelableCallback callback) {
        tilesView.animateTo(x, y, zoomLevel, callback);
    }

    @Override
    public final void animateTo(float x, float y, int zoomLevel) {
        animateTo(x, y, zoomLevel, null);
    }
}
