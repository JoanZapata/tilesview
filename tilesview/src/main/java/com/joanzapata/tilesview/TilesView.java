package com.joanzapata.tilesview;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.joanzapata.tilesview.internal.Tile;
import com.joanzapata.tilesview.internal.TilePool;
import com.joanzapata.tilesview.util.AndroidCompatUtil;
import com.joanzapata.tilesview.util.AndroidCompatUtil.ValueAnimator;
import com.joanzapata.tilesview.util.ScrollAndZoomDetector;

public class TilesView extends View implements ScrollAndZoomDetector.ScrollAndZoomListener, TilePool.TilePoolListener {

    public static final int TILE_SIZE = 256;
    // Zoom level starts at 10, must be 10 plus a power of 2
    // Those MIN and MAX zoom levels only limit the tiles rendering.
    private static final int MAX_ZOOM_LEVEL = 10 + (int) Math.pow(2, 8);
    private static final int MIN_ZOOM_LEVEL = 5;
    private static final int DOUBLE_TAP_DURATION = 400;
    private static final int ANIMATE_TO_DURATION = 600;
    private static final Interpolator DOUBLE_TAP_INTERPOLATOR = new AccelerateDecelerateInterpolator();
    private static final long SCALE_ADJUSTMENT_DURATION = 200;
    public static final int SCALE_TYPE_FLOOR = 1;
    public static final int SCALE_TYPE_CEIL = 2;
    public static final int SCALE_TYPE_ROUND = 3;

    /**
     * Prevent the user from reaching an inappropriate zoom level.
     * 5-9 = content is smaller than the screen
     * 10 = fit the screen
     * > 10 = zoomed in, should be a power of two
     */
    private int userMinZoomLevel = MIN_ZOOM_LEVEL, userMaxZoomLevel = 10 + (int) Math.pow(2, 16);

    /**
     * Add padding to the content.
     */
    private int contentPaddingLeft, contentPaddingTop, contentPaddingRight, contentPaddingBottom;

    /**
     * X and Y offset of the top left corner of the screen in the global image
     */
    private float offsetX, offsetY;

    /**
     * With no overscroll, the content is stuck on the edge of the screen.
     * The overscroll allows to push the content over the edge of the screen.
     */
    private float overscrollLeft, overscrollTop, overscrollRight, overscrollBottom;

    /**
     * Initial scale is 1, scale can't be < 1
     */
    private float scale;

    /**
     * Zoom level is scale * 10 rounded to the nearest integer (e.g. 12 for x1,23)
     */
    private int zoomLevel, zoomLevelWithUserBounds;

    /**
     * Retains all tiles in memory
     */
    private final TilePool tilePool;

    private final Paint debugPaint, metricsPaint;
    private final Paint backgroundPaint;

    private ScrollAndZoomDetector scrollAndZoomDetector;

    private OnViewLoadedCallback onViewLoadedCallback;

    private RectF reusableRectF = new RectF();
    private Rect reusableRect = new Rect();
    private boolean debug = false;
    private ValueAnimator currentAnimator;
    private OnZoomLevelChangedListener onZoomLevelChangedListener;

    private boolean viewAlreadyLoaded = false;

    private TilesViewAdapter adapter;

    public TilesView(Context context) {
        this(context, null);
    }

    public TilesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.tilePool = new TilePool(this);
        this.contentPaddingLeft = 0;
        this.contentPaddingTop = 0;
        this.contentPaddingRight = 0;
        this.contentPaddingBottom = 0;
        this.scrollAndZoomDetector = new ScrollAndZoomDetector(context, this, this);

        clear();

        debugPaint = new Paint();
        debugPaint.setAntiAlias(true);
        debugPaint.setColor(Color.GRAY);
        debugPaint.setTextSize(40);
        debugPaint.setTextAlign(Paint.Align.CENTER);
        debugPaint.setStyle(Paint.Style.STROKE);
        metricsPaint = new Paint(debugPaint);
        metricsPaint.setStyle(Paint.Style.FILL);
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setStyle(Paint.Style.FILL);
        setBackgroundDrawable(getBackground());
    }

    public void setBackground(Drawable background) {
        setBackgroundDrawable(background);
    }

    @Override
    public void setBackgroundColor(int color) {
        setBackgroundDrawable(new ColorDrawable(color));
    }

    @Override
    public void setBackgroundResource(int resid) {
        setBackgroundDrawable(getResources().getDrawable(resid));
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        // Don't set super background at null in super constructor.
        if (backgroundPaint == null) {
            super.setBackgroundDrawable(background);
        } else {
            if (background instanceof ColorDrawable) {
                int backgroundColor = AndroidCompatUtil.getColor(background);
                backgroundPaint.setColor(backgroundColor);
                tilePool.setTilesBackgroundColor(backgroundColor);
            }
            super.setBackgroundDrawable(null);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clear();
    }

    public TilesView clear() {
        if (currentAnimator != null) currentAnimator.cancel();
        adapter = null;
        tilePool.setAdapter(null);
        scale = 1f;
        zoomLevelWithUserBounds = 10;
        zoomLevel = zoomLevelForScale(scale, SCALE_TYPE_ROUND);
        offsetX = -getPaddingLeft() - getContentPaddingLeft();
        offsetY = -getPaddingTop() - getContentPaddingTop();
        viewAlreadyLoaded = false;
        onViewLoadedCallback = null;
        postInvalidate();
        return this;
    }

    public TilesView setDebug(boolean debug) {
        this.debug = debug;
        invalidate();
        return this;
    }

    public TilesView setMinZoomLevel(int minZoomLevel) {
        if (minZoomLevel > 10) {
            minZoomLevel = 10 + (int) Math.round(Math.pow(2, (minZoomLevel - 10)));
        }
        this.userMinZoomLevel = minZoomLevel;
        if (isSized()) {
            applyScaleBounds();
        }
        return this;
    }

    public TilesView setMaxZoomLevel(int maxZoomLevel) {
        if (maxZoomLevel > 10) {
            maxZoomLevel = 10 + (int) Math.round(Math.pow(2, (maxZoomLevel - 10)));
        }
        this.userMaxZoomLevel = maxZoomLevel;
        if (isSized()) {
            applyScaleBounds();
        }
        return this;
    }

    public TilesView setOnZoomLevelChangedListener(OnZoomLevelChangedListener onZoomLevelChangedListener) {
        this.onZoomLevelChangedListener = onZoomLevelChangedListener;
        return this;
    }

    /**
     * Sets a callback that is invoked when this view has finished rendering. The callback will only be invoked once. If
     * this method is called when the view is fully rendered, the callback will be invoked immediately. This event will
     * not fire if the view is continuously changing and never completes loading due to the user constantly interacting
     * with the view.
     */
    public TilesView setOnViewLoadedCallback(OnViewLoadedCallback onViewLoadedCallback) {
        if (viewAlreadyLoaded) {
            onViewLoadedCallback.onViewLoaded();
        } else {
            this.onViewLoadedCallback = onViewLoadedCallback;
        }
        return this;
    }

    public TilesView setContentPadding(int left, int top, int right, int bottom) {
        clear();
        this.contentPaddingLeft = left;
        this.contentPaddingTop = top;
        this.contentPaddingRight = right;
        this.contentPaddingBottom = bottom;
        this.offsetX = -getPaddingLeft() - getContentPaddingLeft();
        this.offsetY = -getPaddingTop() - getContentPaddingTop();
        return this;
    }

    public TilesView setContentPadding(int padding) {
        return setContentPadding(padding, padding, padding, padding);
    }

    public int getMinZoomLevel() {
        int minZoomLevel = this.userMinZoomLevel;
        if (minZoomLevel > 10)
            minZoomLevel = (int) (10 + Math.log(minZoomLevel - 10) / Math.log(2));
        return minZoomLevel;
    }

    public int getMaxZoomLevel() {
        int maxZoomLevel = this.userMaxZoomLevel;
        if (maxZoomLevel > 10)
            maxZoomLevel = (int) (10 + Math.log(maxZoomLevel - 10) / Math.log(2));
        return maxZoomLevel;
    }

    public int getZoomLevel() {
        int zoomLevel = this.zoomLevelWithUserBounds;
        if (zoomLevel > 10)
            zoomLevel = (int) (10 + Math.log(zoomLevel - 10) / Math.log(2));
        return zoomLevel;
    }

    public int getContentPaddingLeft() {
        return contentPaddingLeft;
    }

    public int getContentPaddingTop() {
        return contentPaddingTop;
    }

    public int getContentPaddingRight() {
        return contentPaddingRight;
    }

    public int getContentPaddingBottom() {
        return contentPaddingBottom;
    }

    private boolean intersect(float l1, float t1, float r1, float b1,
                              float l2, float t2, float r2, float b2) {
        return l2 < r1 && l1 < r2
                && t2 < b1 && t1 < b2;
    }

    public void invalidateTiles(float l, float t, float r, float b) {
        Tile[] tiles = tilePool.getTiles();
        for (int i = 0, size = tiles.length; i < size; i++) {
            Tile tile = tiles[i];
            float scaledTileSize = TILE_SIZE / (tile.getZoomLevel() / 10f);
            float tileL = tile.getxIndex() * scaledTileSize;
            float tileT = tile.getyIndex() * scaledTileSize;
            float tileR = tileL + scaledTileSize;
            float tileB = tileT + scaledTileSize;
            if (intersect(l, t, r, b, tileL, tileT, tileR, tileB)) {
                tile.setDeleted(true);
                if (scale != tile.getZoomLevel() / 10f)
                    tile.setBitmap(null);
            }
        }
        tilePool.invalidatePlaceholder();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        boolean viewLoaded = true;
        float contentWidth = getContentWidth();
        float contentHeight = getContentHeight();

        if (debug) {
            metricsPaint.setColor(0xFFF2676B);
            metricsPaint.setStyle(Paint.Style.FILL);
        }
        Paint paddingPaint = debug ? metricsPaint : backgroundPaint;
        canvas.drawRect(0, 0, getPaddingLeft(), getHeight() - getPaddingBottom(), paddingPaint);
        canvas.drawRect(getPaddingLeft(), 0, getWidth(), getPaddingTop(), paddingPaint);
        canvas.drawRect(getWidth() - getPaddingRight(), getPaddingTop(), getWidth(), getHeight(), paddingPaint);
        canvas.drawRect(0, getHeight() - getPaddingBottom(), getWidth() - getPaddingRight(), getHeight(), paddingPaint);

        canvas.clipRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());

        canvas.save();
        canvas.translate((int) -offsetX, (int) -offsetY);

        // Retrieve a placeholder for tiles not yet rendered
        Bitmap placeholder = tilePool.getPlaceholder(contentWidth, contentHeight);
        float placeholderRatio = 0f;
        if (placeholder != null) {
            placeholderRatio = contentWidth / placeholder.getWidth();
        }

        // Find the top left index for the current scale and canvas size
        float zoomDiff = scale / (zoomLevel / 10f);
        float xOffsetOnContent = offsetX / scale;
        float yOffsetOnContent = offsetY / scale;
        float screenWidthOnContent = getWidth() / scale;
        float screenHeightOnContent = getHeight() / scale;
        float tileSizeOnContent = TILE_SIZE / (zoomLevel / 10f);
        int xIndexStart = (int) (xOffsetOnContent / tileSizeOnContent);
        int yIndexStart = (int) (yOffsetOnContent / tileSizeOnContent);
        int xIndexStop = (int) ((xOffsetOnContent + screenWidthOnContent) / tileSizeOnContent);
        int yIndexStop = (int) ((yOffsetOnContent + screenHeightOnContent) / tileSizeOnContent);

        // Adjustments for edge cases
        if (xOffsetOnContent < 0) xIndexStart--;
        if (yOffsetOnContent < 0) yIndexStart--;

        int xGridIndexStart = Math.max(0, xIndexStart);
        int yGridIndexStart = Math.max(0, yIndexStart);
        int xGridIndexStop = Math.min(xIndexStop, (int) Math.floor(contentWidth / tileSizeOnContent));
        int yGridIndexStop = Math.min(yIndexStop, (int) Math.floor(contentHeight / tileSizeOnContent));

        /*
         * Loop through the 2D grid. This loop is a little complex
         * because it starts at the top left corner and reach the
         * center in a spiral like movement. See https://goo.gl/r7As7V
         */
        int xIndex = xIndexStart;
        int yIndex = yIndexStart;
        while (xIndex <= xIndexStop || yIndex <= yIndexStop) {

            while (xIndex <= xIndexStop && xIndex >= xIndexStart) {

                viewLoaded = viewLoaded & drawTile(xIndex, yIndex, canvas,
                        placeholder, zoomDiff, placeholderRatio,
                        xGridIndexStart, xGridIndexStop,
                        yGridIndexStart, yGridIndexStop,
                        contentWidth, contentHeight);

                xIndex++;
            }
            xIndex = xIndexStop;
            yIndex++;
            yIndexStart++;
            if (xIndexStart > xIndexStop || yIndexStart > yIndexStop) break;

            while (yIndex <= yIndexStop && yIndex >= yIndexStart) {

                viewLoaded = viewLoaded & drawTile(xIndex, yIndex, canvas,
                        placeholder, zoomDiff, placeholderRatio,
                        xGridIndexStart, xGridIndexStop,
                        yGridIndexStart, yGridIndexStop,
                        contentWidth, contentHeight);

                yIndex++;
            }
            yIndex = yIndexStop;
            xIndexStop--;
            xIndex--;
            if (xIndexStart > xIndexStop || yIndexStart > yIndexStop) break;

            while (xIndex <= xIndexStop && xIndex >= xIndexStart) {

                viewLoaded = viewLoaded & drawTile(xIndex, yIndex, canvas,
                        placeholder, zoomDiff, placeholderRatio,
                        xGridIndexStart, xGridIndexStop,
                        yGridIndexStart, yGridIndexStop,
                        contentWidth, contentHeight);

                xIndex--;
            }
            xIndex = xIndexStart;
            yIndex--;
            yIndexStop--;
            if (xIndexStart > xIndexStop || yIndexStart > yIndexStop) break;

            while (yIndex <= yIndexStop && yIndex >= yIndexStart) {

                viewLoaded = viewLoaded & drawTile(xIndex, yIndex, canvas,
                        placeholder, zoomDiff, placeholderRatio,
                        xGridIndexStart, xGridIndexStop,
                        yGridIndexStart, yGridIndexStop,
                        contentWidth, contentHeight);

                yIndex--;
            }
            yIndex = yIndexStart;
            xIndexStart++;
            xIndex++;
            if (xIndexStart > xIndexStop || yIndexStart > yIndexStop) break;

        }

        // Render user layer
        if (adapter != null) {
            canvas.save();
            adapter.drawLayer(canvas, scale, contentWidth, contentHeight);
            canvas.restore();
        }

        if (debug && adapter != null) {

            // Draw content padding
            adapter.getBounds(reusableRectF);
            metricsPaint.setStyle(Paint.Style.FILL);
            metricsPaint.setColor(0xFFF5CC70);
            canvas.drawRect(
                    reusableRectF.left - getPaddingLeft(),
                    reusableRectF.top - getPaddingTop(),
                    reusableRectF.left,
                    reusableRectF.bottom + getPaddingBottom(),
                    metricsPaint);
            canvas.drawRect(
                    reusableRectF.right,
                    reusableRectF.top - getPaddingTop(),
                    reusableRectF.right + getPaddingRight(),
                    reusableRectF.bottom + getPaddingBottom(),
                    metricsPaint);
            canvas.drawRect(
                    reusableRectF.left,
                    reusableRectF.top - getPaddingTop(),
                    reusableRectF.right,
                    reusableRectF.top,
                    metricsPaint);
            canvas.drawRect(
                    reusableRectF.left,
                    reusableRectF.bottom,
                    reusableRectF.right,
                    reusableRectF.bottom + getPaddingBottom(),
                    metricsPaint);

            // Draw overscroll
            metricsPaint.setColor(0xFF5992C7);

            // Draw overscroll bars
            metricsPaint.setStyle(Paint.Style.FILL);
            int overscrollBarWidth = 20;
            drawRectIgnoreNegative(canvas,
                    reusableRectF.left - contentPaddingLeft - overscrollLeft,
                    reusableRectF.centerY() - overscrollBarWidth,
                    reusableRectF.left - contentPaddingLeft,
                    reusableRectF.centerY() + overscrollBarWidth,
                    metricsPaint);
            drawRectIgnoreNegative(canvas,
                    reusableRectF.right + contentPaddingRight + overscrollRight,
                    reusableRectF.centerY() - overscrollBarWidth,
                    reusableRectF.right + contentPaddingRight,
                    reusableRectF.centerY() + overscrollBarWidth,
                    metricsPaint);
            drawRectIgnoreNegative(canvas,
                    reusableRectF.centerX() - overscrollBarWidth,
                    reusableRectF.top - contentPaddingTop - overscrollTop,
                    reusableRectF.centerX() + overscrollBarWidth,
                    reusableRectF.top - contentPaddingTop,
                    metricsPaint);
            drawRectIgnoreNegative(canvas,
                    reusableRectF.centerX() - overscrollBarWidth,
                    reusableRectF.bottom + contentPaddingBottom + overscrollBottom,
                    reusableRectF.centerX() + overscrollBarWidth,
                    reusableRectF.bottom + contentPaddingBottom,
                    metricsPaint);

            // Draw bounds
            metricsPaint.setStyle(Paint.Style.STROKE);
            metricsPaint.setStrokeWidth(6);
            reusableRectF.inset(3, 3);
            canvas.drawRect(reusableRectF, metricsPaint);
        }

        canvas.restore();

        if (viewLoaded && onViewLoadedCallback != null) {
            onViewLoadedCallback.onViewLoaded();
            onViewLoadedCallback = null;
            viewAlreadyLoaded = true;
        }
    }

    private boolean drawTile(
            int xIndex, int yIndex,
            Canvas canvas, Bitmap placeholder,
            float zoomDiff, float placeholderRatio,
            int xGridIndexStart, int xGridIndexStop,
            int yGridIndexStart, int yGridIndexStop,
            float contentWidth, float contentHeight) {

        boolean tileLoaded;

        // Compute the current tile position on canvas
        float spread = zoomDiff != 1f ? +1f : 0f;
        float left = xIndex * (float) TILE_SIZE * zoomDiff;
        float top = yIndex * (float) TILE_SIZE * zoomDiff;
        float right = left + TILE_SIZE * zoomDiff + spread;
        float bottom = top + TILE_SIZE * zoomDiff + spread;

        // If this tile is not outside the user content
        if (xIndex >= xGridIndexStart && xIndex <= xGridIndexStop &&
                yIndex >= yGridIndexStart && yIndex <= yGridIndexStop) {

            // Request the tile
            Bitmap tile = tilePool.getTile(zoomLevel, xIndex, yIndex, contentWidth, contentHeight);

            if (tile != null && !tile.isRecycled()) {
                // Draw the tile if any
                reusableRectF.set(left, top, right, bottom);
                canvas.drawBitmap(tile, null, reusableRectF, null);
                tileLoaded = true;
            } else if (placeholder != null && xIndex >= 0 && yIndex >= 0) {
                // Draw the placeholder if any
                reusableRectF.set(left, top, right, bottom);
                float placeholderTileSize = TILE_SIZE / placeholderRatio / scale * zoomDiff;
                reusableRect.set(
                        (int) (xIndex * placeholderTileSize),
                        (int) (yIndex * placeholderTileSize),
                        (int) ((xIndex + 1f) * placeholderTileSize),
                        (int) ((yIndex + 1f) * placeholderTileSize));

                if (reusableRect.right > placeholder.getWidth()) {
                    float rightOffsetOnPlaceholderTile = reusableRect.right - placeholder.getWidth();
                    float rightOffset = rightOffsetOnPlaceholderTile * (TILE_SIZE * zoomDiff) / placeholderTileSize;
                    canvas.drawRect(
                            reusableRectF.right - rightOffset - 1, reusableRectF.top,
                            reusableRectF.right, reusableRectF.bottom,
                            backgroundPaint);
                    reusableRectF.right -= rightOffset;
                    reusableRect.right = placeholder.getWidth();
                }

                if (reusableRect.bottom > placeholder.getHeight()) {
                    float bottomOffsetOnPlaceholderTile = reusableRect.bottom - placeholder.getHeight();
                    float bottomOffset = bottomOffsetOnPlaceholderTile * (TILE_SIZE * zoomDiff) / placeholderTileSize;
                    canvas.drawRect(
                            reusableRectF.left, reusableRectF.bottom - bottomOffset - 1,
                            reusableRectF.right, reusableRectF.bottom,
                            backgroundPaint);
                    reusableRectF.bottom -= bottomOffset;
                    reusableRect.bottom = placeholder.getHeight();
                }

                canvas.drawBitmap(placeholder, reusableRect, reusableRectF, null);
                tileLoaded = false;
            } else {
                // Draw the background otherwise
                canvas.drawRect(left, top, right, bottom, backgroundPaint);
                tileLoaded = false;
            }

            if (debug) {
                int lineSize = 20;
                canvas.drawLine(left, top, left + lineSize, top, debugPaint);
                canvas.drawLine(left, top, left, top + lineSize, debugPaint);
                canvas.drawLine(left, bottom - lineSize, left, bottom, debugPaint);
                canvas.drawLine(left, bottom, left + lineSize, bottom, debugPaint);
                canvas.drawLine(right - lineSize, top, right, top, debugPaint);
                canvas.drawLine(right, top, right, top + lineSize, debugPaint);
                canvas.drawLine(right - lineSize, bottom, right, bottom, debugPaint);
                canvas.drawLine(right, bottom - lineSize, right, bottom, debugPaint);
                canvas.drawText(xIndex + "," + yIndex,
                        (left + right) / 2f,
                        (top + bottom) / 2f + debugPaint.getTextSize() / 4,
                        debugPaint);
                canvas.drawText(zoomLevel + "",
                        right - 30,
                        top + debugPaint.getTextSize() + 5,
                        debugPaint);
            }

        } else {

            // If the current tile is outside user content, draw placeholder
            canvas.drawRect(left, top, right, bottom, backgroundPaint);
            tileLoaded = true;

        }

        return tileLoaded;
    }

    public void setAdapter(TilesViewAdapter tilesViewAdapter) {
        clear();
        viewAlreadyLoaded = false;
        adapter = tilesViewAdapter;
        adapter.attachTilesView(this);
        tilePool.setAdapter(adapter);
        postInvalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        double tilesOnWidth = Math.ceil(w / (TILE_SIZE * 0.9f)) + 1;
        double tilesOnHeight = Math.ceil(h / (TILE_SIZE * 0.9f)) + 1;
        int maxTilesOnScreen = (int) (tilesOnWidth * tilesOnHeight);
        tilePool.setMaxTasks(maxTilesOnScreen);
        applyScaleBounds();
    }

    private void applyScaleBounds() {
        onScaleEnd(getWidth() / 2f, getHeight() / 2f, 1f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return scrollAndZoomDetector.onTouchEvent(event);
    }

    @Override
    public void onDown() {
        if (currentAnimator != null)
            currentAnimator.cancel();
    }

    @Override
    public boolean onScroll(float distanceX, float distanceY) {
        if (adapter != null) {
            offsetX += distanceX;
            offsetY += distanceY;
            adapter.getBounds(reusableRectF);

            // Contraint the scrolling regarding the content bounds, paddings, and overscrolls
            float minOffsetX = -Math.max(
                    getPaddingLeft() + getContentPaddingLeft() + overscrollLeft - reusableRectF.left,
                    getWidth() - getPaddingRight() - getContentPaddingRight() - reusableRectF.right);
            float minOffsetY = -Math.max(
                    getPaddingTop() + getContentPaddingTop() + overscrollTop - reusableRectF.top,
                    getHeight() - getPaddingBottom() - getContentPaddingBottom() - reusableRectF.bottom);
            float maxOffsetX = -Math.min(
                    getWidth() - getPaddingRight() - getContentPaddingRight() - overscrollRight - reusableRectF.right,
                    getPaddingLeft() + getContentPaddingLeft() - reusableRectF.left);
            float maxOffsetY = -Math.min(
                    getHeight() - getPaddingBottom() - getContentPaddingBottom() - overscrollBottom - reusableRectF.bottom,
                    getPaddingTop() + getContentPaddingTop() - reusableRectF.top);
            offsetX = Math.min(Math.max(offsetX, minOffsetX), maxOffsetX);
            offsetY = Math.min(Math.max(offsetY, minOffsetY), maxOffsetY);

            invalidate();
        }
        return true;
    }

    @Override
    public boolean onScale(float scaleFactor, float focusX, float focusY) {

        // Move offsets so that the focus point remains the same
        float newScale = scale * scaleFactor;
        float contentWidthBefore = getWidth() * scale;
        float contentWidthAfter = getWidth() * newScale;
        float contentFocusXBefore = offsetX + focusX;
        float contentFocusXBeforeRatio = contentFocusXBefore / contentWidthBefore;
        float contentFocusXAfter = contentFocusXBeforeRatio * contentWidthAfter;
        float contentHeightBefore = getHeight() * scale;
        float contentHeightAfter = getHeight() * newScale;
        float contentFocusYBefore = offsetY + focusY;
        float contentFocusYBeforeRatio = contentFocusYBefore / contentHeightBefore;
        float contentFocusYAfter = contentFocusYBeforeRatio * contentHeightAfter;

        scale = newScale;
        int newZoomLevelWithoutBounds = zoomLevelForScale(scale, SCALE_TYPE_ROUND);
        int newZoomLevelWithUserBounds = Math.max(Math.min(newZoomLevelWithoutBounds, userMaxZoomLevel), userMinZoomLevel);
        if (zoomLevelWithUserBounds != newZoomLevelWithUserBounds) {
            zoomLevelWithUserBounds = newZoomLevelWithUserBounds;

            if (onZoomLevelChangedListener != null) {
                onZoomLevelChangedListener.onZoomLevelChanged(getZoomLevel());
            }
        }

        onScroll(contentFocusXAfter - contentFocusXBefore, contentFocusYAfter - contentFocusYBefore);
        zoomLevel = Math.min(MAX_ZOOM_LEVEL, Math.max(MIN_ZOOM_LEVEL, newZoomLevelWithoutBounds));
        invalidate();
        return true;
    }

    @Override
    public boolean onDoubleTap(final float focusX, final float focusY) {
        animateScaleTo(zoomLevelForScale(scale * 2f, SCALE_TYPE_ROUND) / 10f, focusX, focusY, DOUBLE_TAP_DURATION);
        return true;
    }

    public void animateTo(final float x, final float y, int zoomLevel, final AnimationCallback callback) {
        if (currentAnimator != null) currentAnimator.cancel();

        if (zoomLevel > 10)
            zoomLevel = (int) (10 + Math.pow(2, zoomLevel - 10));

        currentAnimator = ValueAnimator.ofFloat(0f, 1f)
                .setDuration(ANIMATE_TO_DURATION)
                .setInterpolator(DOUBLE_TAP_INTERPOLATOR)
                .addListener(new ValueAnimator.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel() {
                        if (callback != null) {
                            callback.onAnimationCancel();
                        }
                    }
                }).start();

        final float xScreenCenterOnContent = (offsetX + getWidth() / 2f) / scale;
        final float yScreenCenterOnContent = (offsetY + getHeight() / 2f) / scale;
        final float targetScale = zoomLevel / 10f;
        final float scaleOrigin = scale;
        final float scaleDistance = targetScale - scale;

        Runnable animation = new Runnable() {
            @Override
            public void run() {

                float animatedValue = currentAnimator.getAnimatedValue();
                float animatedValueForXY = DOUBLE_TAP_INTERPOLATOR.getInterpolation(animatedValue);
                float xCurrentOnContent = xScreenCenterOnContent + (x - xScreenCenterOnContent) * animatedValueForXY;
                float yCurrentOnContent = yScreenCenterOnContent + (y - yScreenCenterOnContent) * animatedValueForXY;
                onScale((scaleOrigin + scaleDistance * animatedValue) / scale, 0, 0);
                float xCurrentOffset = xCurrentOnContent * scale - getWidth() / 2f;
                float yCurrentOffset = yCurrentOnContent * scale - getHeight() / 2f;
                onScroll(xCurrentOffset - offsetX, yCurrentOffset - offsetY);

                if (currentAnimator.isRunning()) {
                    ViewCompat.postOnAnimation(TilesView.this, this);
                } else {
                    if (animatedValue != 1f) {
                        float xFinalOnContent = xScreenCenterOnContent + (x - xScreenCenterOnContent);
                        float yFinalOnContent = yScreenCenterOnContent + (y - yScreenCenterOnContent);
                        onScale((scaleOrigin + scaleDistance) / scale, 0, 0);
                        float xFinalOffset = xFinalOnContent * scale - getWidth() / 2f;
                        float yFinalOffset = yFinalOnContent * scale - getHeight() / 2f;
                        onScroll(xFinalOffset - offsetX, yFinalOffset - offsetY);
                    }

                    if (callback != null) {
                        callback.onAnimationFinish();
                    }
                }

                invalidate();
            }
        };

        ViewCompat.postOnAnimation(this, animation);
    }

    public void getPositionInView(float x, float y, PointF position) {
        position.set(x * scale - offsetX, y * scale - offsetY);
    }

    private void animateScaleTo(final float newScale, final float focusXOnScreen, final float focusYOnScreen, final long duration) {
        if (currentAnimator != null) currentAnimator.cancel();
        currentAnimator = ValueAnimator.ofFloat(scale, newScale)
                .setDuration(duration)
                .setInterpolator(DOUBLE_TAP_INTERPOLATOR)
                .start();
        Runnable animation = new Runnable() {
            @Override
            public void run() {
                float animatedValue = currentAnimator.getAnimatedValue();
                onScale(animatedValue / scale, focusXOnScreen, focusYOnScreen);
                if (currentAnimator.isRunning()) {
                    ViewCompat.postOnAnimation(TilesView.this, this);
                } else {
                    if (animatedValue != newScale) {
                        onScale(newScale / scale, focusXOnScreen, focusYOnScreen);
                    }

                    onScaleEnd(focusXOnScreen, focusYOnScreen, 0f);
                }
                invalidate();
            }
        };
        ViewCompat.postOnAnimation(TilesView.this, animation);
    }

    @Override
    public void onScaleEnd(float focusX, float focusY, float lastScaleFactor) {
        int bestZoomLevel = zoomLevelForScale(scale, lastScaleFactor >= 1 ? SCALE_TYPE_CEIL : SCALE_TYPE_FLOOR);
        bestZoomLevel = Math.min(Math.max(bestZoomLevel, userMinZoomLevel), userMaxZoomLevel);
        if (scale != bestZoomLevel / 10f) {
            animateScaleTo(bestZoomLevel / 10f, focusX, focusY, SCALE_ADJUSTMENT_DURATION);
        }
    }

    @Override
    public void onSingleTap(float screenX, float screenY) {
        if (adapter != null) {
            float contentWidth = getContentWidth();
            float contentHeight = getContentHeight();
            float contentX = (screenX + offsetX) / scale;
            float contentY = (screenY + offsetY) / scale;
            adapter.onClick(
                    contentX / contentWidth,
                    contentY / contentHeight,
                    contentWidth, contentHeight, scale);
        }
    }

    /**
     * Return an appropriate zoom level for the given scale
     */
    public int zoomLevelForScale(float scale, int scaleType) {
        if (scale == 1) {
            return 10;
        } else if (scale < 1) {
            return Math.round(scale * 10f);
        }
        double scaleFrom0x10 = Math.round(scale * 10) - 10d;
        double exactValue = Math.log(scaleFrom0x10) / Math.log(2);
        int roundedValue = (int) (scaleType == SCALE_TYPE_FLOOR ? Math.floor(exactValue) :
                scaleType == SCALE_TYPE_CEIL ? Math.ceil(exactValue) :
                        Math.round(exactValue));
        return (int) (10 + Math.pow(2, roundedValue));
    }

    @Override
    public void onTileRendered(Tile tile) {
        postInvalidate();
    }

    private boolean isSized() {
        return getWidth() != 0 && getHeight() != 0;
    }

    public void setZoomLevel(int zoomLevel) {
        if (zoomLevel < getMinZoomLevel() || zoomLevel > getMaxZoomLevel()) {
            throw new IllegalArgumentException("Zoom level should be between " + getMinZoomLevel() + " and " + getMaxZoomLevel() + ".");
        }
        if (zoomLevel > 10)
            zoomLevel = (int) (10 + Math.pow(2, zoomLevel - 10));
        animateScaleTo(zoomLevel / 10f, getWidth() / 2f, getHeight() / 2f, SCALE_ADJUSTMENT_DURATION);

    }

    public float getContentWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight() - getContentPaddingLeft() - getContentPaddingRight();
    }

    public float getContentHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom() - getContentPaddingTop() - getContentPaddingBottom();
    }

    public float getScale() {
        return scale;
    }

    private void drawRectIgnoreNegative(Canvas canvas, float left, float top, float right, float bottom, Paint paint) {
        float actualLeft = left < right ? left : right;
        float actualTop = top < bottom ? top : bottom;
        float actualRight = right > left ? right : left;
        float actualBottom = bottom > top ? bottom : top;
        canvas.drawRect(actualLeft, actualTop, actualRight, actualBottom, paint);
    }

    public void setOverscrollTop(float overscrollTop) {
        this.overscrollTop = overscrollTop;
    }

    public void setOverscrollRight(float overscrollRight) {
        this.overscrollRight = overscrollRight;
    }

    public void setOverscrollLeft(float overscrollLeft) {
        this.overscrollLeft = overscrollLeft;
    }

    public void setOverscrollBottom(float overscrollBottom) {
        this.overscrollBottom = overscrollBottom;
    }

    public void setOverscroll(float left, float top, float right, float bottom) {
        setOverscrollLeft(left);
        setOverscrollTop(top);
        setOverscrollRight(right);
        setOverscrollBottom(bottom);
    }

    public void setOverscroll(float overscroll) {
        setOverscroll(overscroll, overscroll, overscroll, overscroll);
    }
}
