package com.joanzapata.tilesview.internal;

import android.graphics.Bitmap;

public class Tile {

    int zoomLevel;
    int x;
    int y;
    Bitmap bitmap;

    public Tile(int zoomLevel, int x, int y) {
        this.zoomLevel = zoomLevel;
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tile tile = (Tile) o;

        if (zoomLevel != tile.zoomLevel) return false;
        if (x != tile.x) return false;
        return y == tile.y;

    }

    @Override
    public int hashCode() {
        int result = zoomLevel;
        result = 31 * result + x;
        result = 31 * result + y;
        return result;
    }
}
