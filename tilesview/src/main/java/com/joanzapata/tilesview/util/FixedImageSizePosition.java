package com.joanzapata.tilesview.util;

import android.graphics.PointF;
import com.joanzapata.tilesview.TilesView;

public class FixedImageSizePosition {
    private final TilesView tilesView;
    private final float sourceWidth;
    private final float sourceHeight;

    public FixedImageSizePosition(TilesView tilesView, float sourceWidth, float sourceHeight) {
        this.tilesView = tilesView;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    public PointF getPosition(float x, float y) {
        float xDiff, yDiff;
        float xFactor, yFactor;
        float scaledSourceHeight = tilesView.getContentWidth() * sourceHeight / sourceWidth;
        if (scaledSourceHeight <= tilesView.getContentHeight()) {
            xDiff = 0f;
            yDiff = -(tilesView.getContentHeight() - scaledSourceHeight) / 2f / tilesView.getContentHeight();
            xFactor = 1f;
            yFactor = tilesView.getContentHeight() / scaledSourceHeight;
        } else {
            float scaledSourceWidth = tilesView.getContentHeight() * sourceWidth / sourceHeight;
            xDiff = -(tilesView.getContentWidth() - scaledSourceWidth) / 2f / tilesView.getContentWidth();
            yDiff = 0f;
            xFactor = tilesView.getContentWidth() / scaledSourceWidth;
            yFactor = 1f;
        }

        float contentX = (x / (xFactor * sourceWidth) - xDiff) * tilesView.getContentWidth();
        float contentY = (y / (yFactor * sourceHeight) - yDiff) * tilesView.getContentHeight();

        return tilesView.getPositionInView(contentX, contentY);
    }
}
