package com.vibin.billy.swipeable;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

/**
 * Calculates the dimensions of only the available window to our app
 */
class WindowUtils {
    public static WindowDimens getSimpleWindowDimens(Context context) {

        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = windowManager.getDefaultDisplay();

        return getAvailableWindowDimens(display, metrics);

    }

    // calculates the dimensions of only the available window to our app
    private static WindowDimens getAvailableWindowDimens(Display display, DisplayMetrics metrics) {

        // available window metrics
        display.getMetrics(metrics);

        return new WindowDimens(
                metrics.widthPixels,
                metrics.heightPixels,
                0,
                0
        );

    }
}