package com.joanzapata.tilesview;

/**
 * Callback interface for when the view has finished rendering. This occurs after all tiles required to render the
 * view have been loaded. This event will not fire if the view is continuously changing and never completes loading
 * due to the user constantly interacting with the view.
 */
public interface OnViewLoadedCallback {
    /**
     * Called when the view has finished rendering. This will only be called once. You must request another callback if
     * you want to be notified again.
     */
    void onViewLoaded();
}
