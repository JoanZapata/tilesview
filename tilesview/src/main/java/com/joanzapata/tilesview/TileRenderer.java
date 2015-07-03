package com.joanzapata.tilesview;

import android.graphics.Canvas;

public interface TileRenderer {
    void renderTile(Canvas canvas,
            float x, float y,
            float width, float height,
            float overallWidth, float overallHeight);
}
