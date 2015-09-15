package com.joanzapata.tilesview.sample.utils;

import android.content.Context;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.applyDimension;

public final class AndroidUtils {

    /** Util method to convert a DP value in pixels */
    public static float dp(Context context, float dp) {
        return applyDimension(COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

}
