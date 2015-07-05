package com.joanzapata.tilesview;

import android.graphics.Canvas;

public interface TileRenderer {
    void renderTile(Canvas canvas,
            float xRatio, float yRatio,
            float widthRatio, float heightRatio,
            float contentInitialWidth, float contentInitialHeight);
}
