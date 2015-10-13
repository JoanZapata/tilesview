package com.joanzapata.tilesview.adapter;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;

import com.joanzapata.tilesview.AnimationCallback;
import com.joanzapata.tilesview.TilesView;
import com.joanzapata.tilesview.TilesViewAdapter;

public abstract class DefaultAdapter implements TilesViewAdapter {

    private TilesView tilesView;

    @Override
    public void attachTilesView(TilesView tilesView) {
        this.tilesView = tilesView;
    }

    @Override
    public boolean isThreadSafe() {
        // Default value, can be overridden
        return false;
    }

    @Override
    public void getBounds(RectF bounds) {
        bounds.set(0, 0,
                tilesView.getContentWidth() * tilesView.getScale(),
                tilesView.getContentHeight() * tilesView.getScale());
    }

    @Override
    public abstract void drawTile(Canvas canvas, float xRatio, float yRatio, float widthRatio, float heightRatio, float contentInitialWidth, float contentInitialHeight, float scale);

    @Override
    public void onClick(float xRatio, float yRatio, float contentInitialWidth, float contentInitialHeight, float scale) {
        // Default do nothing.
    }

    @Override
    public final void getPosition(float x, float y, PointF position) {
        tilesView.getPositionInView(x, y, position);
    }

    @Override
    public void drawLayer(Canvas canvas, float scale, float contentInitialWidth, float contentInitialHeight) {
        // Default do nothing
    }

    @Override
    public final void animateTo(float x, float y, int zoomLevel, AnimationCallback callback) {
        tilesView.animateTo(x, y, zoomLevel, callback);
    }

    @Override
    public final void animateTo(float x, float y, int zoomLevel) {
        animateTo(x, y, zoomLevel, null);
    }

    @Override
    public void animateTo(float x, float y, AnimationCallback callback) {
        animateTo(x, y, tilesView.getZoomLevel(), callback);
    }

    @Override
    public void animateTo(float x, float y) {
        animateTo(x, y, tilesView.getZoomLevel(), null);
    }
}
