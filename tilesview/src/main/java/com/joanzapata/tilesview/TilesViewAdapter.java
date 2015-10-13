package com.joanzapata.tilesview;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;

/**
 * A TilesViewAdapter is the class to implement when you
 * want to configure and control a TilesView.
 * <br>
 * In most cases you don't need to implement it directly,
 * but instead subclass {@link com.joanzapata.tilesview.adapter.DefaultAdapter}
 * or {@link com.joanzapata.tilesview.adapter.FixedSizeAdapter} depending
 * on your usecase.
 */
public interface TilesViewAdapter {

    /**
     * This is called by TilesView and should not be called by the user.
     * Used to keep a reference on the associated TilesView for animations.
     */
    void attachTilesView(TilesView tilesView);

    /**
     * Provides the TilesView with the bounds of the content.
     * These bounds are used to determine the user's freedom
     * when scrolling the content.
     * Bounds should be set regarding the size of the content at the
     * current scale.
     * By default you would probably want to implement it like this
     * <pre>
     * {@code
     *  bounds.set(0, 0,
     *      tilesView.getContentWidth() * tilesView.getScale(),
     *      tilesView.getContentHeight() * tilesView.getScale());
     * }
     * </pre>
     * @param bounds The bounds to set with your values.
     */
    void getBounds(RectF bounds);

    /**
     * @return True if your implementation of {@link #drawTile(Canvas, float, float, float, float, float, float, float)} is thread safe, in which case the rendering
     * will be significantly faster. Default is false.
     */
    boolean isThreadSafe();

    /**
     * TilesView calls this method to render each created Tile with your content.
     * This is the critical piece of the Adapter and it should be properly implemented.
     * This method run on a background thread.
     * <br>
     * xRatio, yRatio, widthRatio and heightRatio are the bounds of the tile in your content
     * relative to contentInitialWidth and contentInitialHeight.
     * <br>
     * You should draw the tile in the canvas at 0,0 and fill the canvas. You should use
     * canvas.getWidth() and canvas.getHeight() to determine the destination bounds.
     * @param canvas               The canvas on which you should draw.
     * @param xRatio               The X position of the current tile in your content, between 0 and 1, relative to contentInitialWidth.
     * @param yRatio               The Y position of the current tile in your content, between 0 and 1, relative to contentInitialHeight.
     * @param widthRatio           The width of the current tile in your content, between 0 and 1, relative to contentInitialWidth.
     * @param heightRatio          The height of the current tile in your content, between 0 and 1, relative to contentInitialHeight.
     * @param contentInitialWidth  Initial available width for the content.
     * @param contentInitialHeight Initial available height for the content.
     * @param scale                This is actually redundant. It could be obtained using canvas.getWidth()/(widthRatio * contentInitialWidth),
     *                             but depending on the way you decide to implement this method you might need it explicitly, so it's
     *                             provided for clarity.
     */
    void drawTile(Canvas canvas,
            float xRatio, float yRatio,
            float widthRatio, float heightRatio,
            float contentInitialWidth, float contentInitialHeight,
            float scale);

    /**
     * Called when the user click on the TilesView.
     * @param xRatio               The X position of the user click in your content, between 0 and 1, relative to contentInitialWidth.
     * @param yRatio               The Y position of the user click in your content, between 0 and 1, relative to contentInitialHeight.
     * @param contentInitialWidth  Initial available width for the content.
     * @param contentInitialHeight Initial available height for the content.
     * @param scale                The current actual scale of the view, 1 means initial zoom. (this is not the zoom level)
     */
    void onClick(float xRatio, float yRatio, float contentInitialWidth, float contentInitialHeight, float scale);

    /**
     * Translate an offset on the TilesView to an offset in the content, regarding the current scale and scroll.
     * @param x        The X position on the TilesView.
     * @param y        The Y position on the TilesView.
     * @param position The PointF that will be filled with the result. (This is a performance improvement, it
     *                 allows you to reuse a PointF instance instead of creating a new one each time you call
     *                 this method.)
     */
    void getPosition(float x, float y, PointF position);

    /**
     * Draw something in the TilesView on top of the tiles. This is called on the UI
     * thread on every frame so be careful not to do expensive computation.
     * <br>
     * The canvas has already been adjusted to the current scroll, which means (0,0) is the top left
     * corner of the content, no matter the padding, scale and scroll currently applied to the TilesView.
     * You should only draw between 0 and contentInitialWidth/Height on the X/Y axis.
     * @param canvas               The canvas on which to draw.
     * @param scale                The current scale of the TilesView, 1f means initial size.
     * @param contentInitialWidth  Initial available width for the content.
     * @param contentInitialHeight Initial available height for the content.
     */
    void drawLayer(Canvas canvas, float scale, float contentInitialWidth, float contentInitialHeight);

    /**
     * Animate the center of the TilesView to the given offset.
     * @param x         The X position on which to center the screen, from 0 to 1, relative to the content initial width.
     * @param y         The Y position on which to center the screen, from 0 to 1, relative to the content initial width.
     * @param zoomLevel The target zoomLevel.
     * @param callback  A callback to be notified when the animation ends or is cancelled.
     */
    void animateTo(float x, float y, int zoomLevel, AnimationCallback callback);

    /**
     * Same as {@link #animateTo(float, float, int, AnimationCallback)} without changing the zoom level.
     */
    void animateTo(float x, float y, AnimationCallback callback);

    /**
     * Same as {@link #animateTo(float, float, int, AnimationCallback)} without a callback.
     */
    void animateTo(float x, float y, int zoomLevel);

    /**
     * Same as {@link #animateTo(float, float, int, AnimationCallback)} without zoom and callback.
     */
    void animateTo(float x, float y);

}
