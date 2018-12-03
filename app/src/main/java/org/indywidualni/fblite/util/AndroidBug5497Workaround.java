package org.indywidualni.fblite.util;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.FrameLayout;

import org.indywidualni.fblite.MyApplication;

public class AndroidBug5497Workaround {

    // For more information, see https://code.google.com/p/android/issues/detail?id=5497
    // To use this class, simply invoke assistActivity() on an Activity that already has its content view set.

    public static void assistActivity(Activity activity) {
        new AndroidBug5497Workaround(activity);
    }

    private final View mChildOfContent;
    private int usableHeightPrevious;

    @SuppressWarnings("FieldCanBeLocal")
    private FrameLayout.LayoutParams frameLayoutParams;

    private final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
            MyApplication.getContextOfApplication());

    private AndroidBug5497Workaround(Activity activity) {
        FrameLayout content = activity.findViewById(android.R.id.content);
        mChildOfContent = content.getChildAt(0);
        mChildOfContent.getViewTreeObserver().addOnGlobalLayoutListener(this::possiblyResizeChildOfContent);
        frameLayoutParams = (FrameLayout.LayoutParams) mChildOfContent.getLayoutParams();
    }

    private void possiblyResizeChildOfContent() {
        int usableHeightNow = computeUsableHeight();
        if (usableHeightNow != usableHeightPrevious) {
            int usableHeightSansKeyboard = mChildOfContent.getRootView().getHeight();
            int heightDifference = usableHeightSansKeyboard - usableHeightNow;
            if (heightDifference > (usableHeightSansKeyboard / 4)) {
                // keyboard probably just became visible
                frameLayoutParams.height = usableHeightSansKeyboard - heightDifference;
            } else {
                // keyboard probably just became hidden
                frameLayoutParams.height = usableHeightSansKeyboard;
            }
            mChildOfContent.requestLayout();
            usableHeightPrevious = usableHeightNow;
        }
    }

    private int computeUsableHeight() {
        Rect r = new Rect();
        mChildOfContent.getWindowVisibleDisplayFrame(r);

        // if transparent navigation enabled avoid too large bottom padding
        if (preferences.getBoolean("transparent_nav", false))
            return (r.bottom - r.top + 144);

        // additional 48 added for better text editing
        return (r.bottom - r.top + 48);
    }

}