package com.joanzapata.tilesview;

/**
 * Callback for animations on the TilesView.
 * @see TilesViewAdapter#animateTo(float, float, int, AnimationCallback)
 */
public interface AnimationCallback {

    /**
     * Called when the animation is cancelled by the user before
     * it has reached its target. This happens if he tries to drag
     * or zoom the TilesView during the animation.
     */
    void onAnimationCancel();

    /**
     * Called when the animation has reached its final position and/or
     * zoom level.
     */
    void onAnimationFinish();
}
