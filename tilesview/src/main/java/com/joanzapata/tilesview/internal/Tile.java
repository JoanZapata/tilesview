package com.joanzapata.tilesview.internal;

import android.graphics.Bitmap;

public class Tile {

    public static final int STATE_RENDERING = 0;
    public static final int STATE_RENDERED = 1;

    private Bitmap bitmap;

    private int state;

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

}
