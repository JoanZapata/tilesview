package com.joanzapata.tilesview.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.animation.Interpolator;

public class AndroidCompatUtil {

    private static Bitmap mBitmap;
    private static Canvas mCanvas;
    private static Rect mBounds;

    public static int getColor(Drawable drawable) {
        int color = Color.BLACK;
        if (drawable instanceof ColorDrawable) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                if (mBitmap == null) {
                    mBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                    mCanvas = new Canvas(mBitmap);
                    mBounds = new Rect();
                }
                // If the ColorDrawable makes use of its bounds in the draw method,
                // we may not be able to get the color we want. This is not the usual
                // case before Ice Cream Sandwich (4.0.1 r1).
                // Yet, we change the bounds temporarily, just to be sure that we are
                // successful.
                ColorDrawable colorDrawable = (ColorDrawable) drawable;
                Rect beforeBounds = colorDrawable.getBounds();
                mBounds.set(beforeBounds);
                colorDrawable.setBounds(0, 0, 1, 1);
                colorDrawable.draw(mCanvas);
                color = mBitmap.getPixel(0, 0);
                colorDrawable.setBounds(beforeBounds);
            } else {
                color = ((ColorDrawable) drawable).getColor();
            }
        }
        return color;
    }


    public static class ValueAnimator {

        private long startTime;
        private float start, stop;
        private boolean running;
        private AnimatorListenerAdapter animatorListenerAdapter;
        private Interpolator interpolator;
        private long duration;

        private ValueAnimator(float start, float stop) {
            this.start = start;
            this.stop = stop;
            running = true;
        }

        public void cancel() {
            if (isOver()) return;
            running = false;
            if (animatorListenerAdapter != null)
                animatorListenerAdapter.onAnimationCancel();
        }

        public ValueAnimator setDuration(long duration) {
            this.duration = duration;
            return this;
        }

        public ValueAnimator setInterpolator(Interpolator interpolator) {
            this.interpolator = interpolator;
            return this;
        }

        public ValueAnimator addListener(AnimatorListenerAdapter animatorListenerAdapter) {
            this.animatorListenerAdapter = animatorListenerAdapter;
            return this;
        }

        public void start() {
            startTime = System.currentTimeMillis();
        }

        public float getAnimatedValue() {
            float progress = Math.min(Math.max((System.currentTimeMillis() - startTime) / (float) duration, 0f), 1f);
            float interpolatedProgress = interpolator.getInterpolation(progress);
            return start + interpolatedProgress * (stop - start);
        }

        public boolean isRunning() {
            return running && !isOver();
        }

        private boolean isOver() {
            return System.currentTimeMillis() >= (startTime + duration);
        }

        public static ValueAnimator ofFloat(float start, float stop) {
            return new ValueAnimator(start, stop);
        }

        public interface AnimatorListenerAdapter {
            void onAnimationCancel();
        }
    }
}