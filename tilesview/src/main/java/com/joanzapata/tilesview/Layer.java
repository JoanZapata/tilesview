package com.joanzapata.tilesview;

import android.graphics.Canvas;

public interface Layer {
    void renderLayer(Canvas canvas, float scale, float overallWidth, float overallHeight);
}
