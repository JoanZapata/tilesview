package com.joanzapata.tilesview.util;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class ScrollAndZoomDetector implements GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {

    private final ScrollAndZoomListener scrollAndZoomListener;

    private final GestureDetector gestureDetector;

    private final ScaleGestureDetector scaleGestureDetector;

    public ScrollAndZoomDetector(Context context, ScrollAndZoomListener scrollAndZoomListener) {
        this.gestureDetector = new GestureDetector(context, this);
        this.gestureDetector.setIsLongpressEnabled(false);
        this.scaleGestureDetector = new ScaleGestureDetector(context, this);
        this.scrollAndZoomListener = scrollAndZoomListener;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return scrollAndZoomListener.onScroll(distanceX, distanceY);
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) | scaleGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        return scrollAndZoomListener.onScale(detector.getScaleFactor());
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        scrollAndZoomListener.onScaleEnd();
    }

    public interface ScrollAndZoomListener {
        boolean onScroll(float distanceX, float distanceY);

        boolean onScale(float scaleFactor);

        void onScaleEnd();
    }

}
