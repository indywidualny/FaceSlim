package org.indywidualni.fblite.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class Dimension {

    // get status bar height (needed for transparent nav bar)
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    // get navigation bar height
    public static int getNavigationBarHeight(Context context, int orientation) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier(orientation == Configuration.ORIENTATION_PORTRAIT ?
                "navigation_bar_height" : "navigation_bar_height_landscape", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    // window's height minus navbar minus extra top padding, all divided by density
    public static int heightForFixedFacebookNavbar(Context context) {
        final int navbar = getNavigationBarHeight(context, context.getResources().getConfiguration().orientation);
        final float density = context.getResources().getDisplayMetrics().density;
        return (int) ((context.getResources().getDisplayMetrics().heightPixels - navbar - 44) / density);
    }

    public static FrameLayout.LayoutParams getParamsAppMsg(Context context) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, getStatusBarHeight(context), 0, 0);
        return layoutParams;
    }

}
