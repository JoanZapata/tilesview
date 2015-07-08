package com.joanzapata.tilesview.internal;

import android.graphics.Bitmap;

public class Tile {

    public static final int STATE_RENDERING = 0;
    public static final int STATE_RENDERED = 1;

    private Bitmap bitmap;

    private int state;

    private final int xIndex, yIndex, zoomLevel;

    public Tile(int xIndex, int yIndex, int zoomLevel) {
        this.xIndex = xIndex;
        this.yIndex = yIndex;
        this.zoomLevel = zoomLevel;
    }

    /**
     * An LRU cache is built on top
     * of these properties.
     */
    private Tile olderTile, newerTile;

    private volatile boolean deleted;

    public int getxIndex() {
        return xIndex;
    }

    public int getyIndex() {
        return yIndex;
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Tile becomeMRU(Tile lastMRU) {
        if (newerTile != null)
            newerTile.olderTile = olderTile;
        if (olderTile != null)
            olderTile.newerTile = newerTile;
        newerTile = null;
        if (lastMRU != this) {
            olderTile = lastMRU;
            if (lastMRU != null)
                lastMRU.newerTile = this;
        }
        return this;
    }

    public Tile removeAndGetNewLRU() {
        Tile newLRU = newerTile;
        newerTile.olderTile = null;
        newerTile = null;
        olderTile = null;
        return newLRU;
    }

    public Tile getNewerTile() {
        return newerTile;
    }
}
