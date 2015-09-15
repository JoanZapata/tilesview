package com.joanzapata.tilesview.sample.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class POI {

    public final Bitmap bitmap;
    public final float offsetX, offsetY;
    public final float deltaX, deltaY;
    public final String name;

    public POI(String name, Context context, int bitmapRes, float offsetX, float offsetY, float yAnchorRatio) {
        this.bitmap = BitmapFactory.decodeResource(context.getResources(), bitmapRes);
        this.deltaX = -bitmap.getWidth() * 0.5f;
        this.deltaY = -bitmap.getHeight() * yAnchorRatio;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.name = name;
    }

    public boolean contains(float x, float y, float scale) {
        float distance = (float) Math.sqrt((offsetX - x) * (offsetX - x) + (offsetY - y) * (offsetY - y));
        return distance <= bitmap.getWidth() / scale / 2f;
    }
}