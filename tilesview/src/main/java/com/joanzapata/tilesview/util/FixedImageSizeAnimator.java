package com.joanzapata.tilesview.util;

import com.joanzapata.tilesview.TilesView;

public class FixedImageSizeAnimator {

    private final TilesView tilesView;

    private final float sourceWidth, sourceHeight;

    public FixedImageSizeAnimator(TilesView tilesView, float sourceWidth, float sourceHeight) {
        this.tilesView = tilesView;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    public void animateTo(float x, float y) {
        float contentWidth = tilesView.getContentWidth();
        float contentHeight = tilesView.getContentHeight();
        float xDiff, yDiff, factor;
        float scaledSourceHeight = contentWidth * sourceHeight / sourceWidth;
        if (scaledSourceHeight <= contentHeight) {
            factor = scaledSourceHeight / sourceHeight;
            xDiff = 0f;
            yDiff = (contentHeight - scaledSourceHeight) / 2f;
        } else {
            float scaledSourceWidth = contentHeight * sourceWidth / sourceHeight;
            factor = scaledSourceWidth / sourceWidth;
            xDiff = (contentWidth - scaledSourceWidth) / 2f;
            yDiff = 0f;
        }

        tilesView.animateTo(xDiff + x * factor, yDiff + y * factor);
    }
}
