package com.joanzapata.tilesview.util;

import com.joanzapata.tilesview.OnContentTappedListener;

public abstract class FixedImageSizeTappedListener implements OnContentTappedListener {

    private final float sourceWidth, sourceHeight;

    protected FixedImageSizeTappedListener(float sourceWidth, float sourceHeight) {
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    @Override
    public final void onContentTapped(float xRatio, float yRatio, float contentInitialWidth, float contentInitialHeight, float scale) {

        // Try using source width as reference
        float xDiff, yDiff;
        float xFactor, yFactor;
        float scaledSourceHeight = contentInitialWidth * sourceHeight / sourceWidth;
        float initialScale;
        if (scaledSourceHeight <= contentInitialHeight) {
            initialScale = contentInitialWidth / sourceWidth;
            xDiff = 0f;
            yDiff = -(contentInitialHeight - scaledSourceHeight) / 2f / contentInitialHeight;
            xFactor = 1f;
            yFactor = contentInitialHeight / scaledSourceHeight;
        } else {
            initialScale = contentInitialHeight / sourceHeight;
            float scaledSourceWidth = contentInitialHeight * sourceWidth / sourceHeight;
            xDiff = -(contentInitialWidth - scaledSourceWidth) / 2f / contentInitialWidth;
            yDiff = 0f;
            xFactor = contentInitialWidth / scaledSourceWidth;
            yFactor = 1f;
        }

        float contentX = (xRatio + xDiff) * xFactor * sourceWidth;
        float contentY = (yRatio + yDiff) * yFactor * sourceHeight;

        if (contentX < 0 || contentX > sourceWidth
                || contentY < 0 || contentY > sourceHeight)
            return;

        contentTapped(contentX, contentY, initialScale * scale);
    }

    protected abstract void contentTapped(float x, float y, float scale);

}
