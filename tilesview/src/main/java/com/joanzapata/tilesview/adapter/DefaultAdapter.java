package com.joanzapata.tilesview.adapter;

import android.graphics.Canvas;
import android.graphics.PointF;
import com.joanzapata.tilesview.AnimationCallback;
import com.joanzapata.tilesview.TilesView;
import com.joanzapata.tilesview.TilesViewAdapter;

public abstract class DefaultAdapter implements TilesViewAdapter {

    private TilesView tilesView;
    private int minZoomLevel = DEFAULT_MIN_ZOOM_LEVEL;
    private int maxZoomLevel = DEFAULT_MAX_ZOOM_LEVEL;
    private float overscrollTop, overscrollRight, overscrollLeft, overscrollBottom;

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
    public float getOverscrollLeft() {
        return overscrollLeft;
    }

    @Override
    public float getOverscrollRight() {
        return overscrollRight;
    }

    @Override
    public float getOverscrollTop() {
        return overscrollTop;
    }

    @Override
    public float getOverscrollBottom() {
        return overscrollBottom;
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

    public void setMinZoomLevel(int minZoomLevel) {
        this.minZoomLevel = minZoomLevel;
    }

    public void setMaxZoomLevel(int maxZoomLevel) {
        this.maxZoomLevel = maxZoomLevel;
    }

    public void setOverscrollTop(float overscrollTop) {
        this.overscrollTop = overscrollTop;
    }

    public void setOverscrollRight(float overscrollRight) {
        this.overscrollRight = overscrollRight;
    }

    public void setOverscrollLeft(float overscrollLeft) {
        this.overscrollLeft = overscrollLeft;
    }

    public void setOverscrollBottom(float overscrollBottom) {
        this.overscrollBottom = overscrollBottom;
    }

    public void setOverscroll(float left, float top, float right, float bottom){
        setOverscrollLeft(left);
        setOverscrollTop(top);
        setOverscrollRight(right);
        setOverscrollBottom(bottom);
    }

    public void setOverscroll(float overscroll){
        setOverscroll(overscroll, overscroll, overscroll, overscroll);
    }
}
