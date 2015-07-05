package com.joanzapata.tilesview.util;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.OverScroller;

public class ScrollAndZoomDetector implements GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {

    private final ScrollAndZoomListener scrollAndZoomListener;

    private final GestureDetector gestureDetector;

    private final ScaleGestureDetector scaleGestureDetector;

    private final OverScroller overScroller;

    private final Context context;
    private final View referenceView;

    public ScrollAndZoomDetector(Context context, View referenceView, ScrollAndZoomListener scrollAndZoomListener) {
        this.context = context;
        this.referenceView = referenceView;
        this.gestureDetector = new GestureDetector(context, this);
        this.gestureDetector.setIsLongpressEnabled(false);
        this.scaleGestureDetector = new ScaleGestureDetector(context, this);
        this.scrollAndZoomListener = scrollAndZoomListener;
        this.overScroller = new OverScroller(context);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (overScroller != null)
            overScroller.forceFinished(true);
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
    public boolean onFling(final MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Runnable animation = new Runnable() {

            float currX = e1.getX();
            float currY = e1.getY();

            @Override
            public void run() {
                if (overScroller.computeScrollOffset() && !overScroller.isFinished()) {
                    scrollAndZoomListener.onScroll(
                            currX - overScroller.getCurrX(),
                            currY - overScroller.getCurrY());
                    currX = overScroller.getCurrX();
                    currY = overScroller.getCurrY();
                    referenceView.postOnAnimation(this);
                }
            }
        };

        overScroller.fling((int) e1.getX(), (int) e1.getY(),
                (int) velocityX, (int) velocityY,
                -Integer.MAX_VALUE, Integer.MAX_VALUE,
                -Integer.MAX_VALUE, Integer.MAX_VALUE);

        referenceView.postOnAnimation(animation);
        return true;
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
