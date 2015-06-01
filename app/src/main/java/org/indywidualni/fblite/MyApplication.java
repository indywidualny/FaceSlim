package org.indywidualni.fblite;

import android.app.Application;
import android.content.Context;

public class MyApplication extends Application {

    // context of application for non context classes
    private static Context mContext;

    // is MainActivity active right now
    private static boolean activityVisible;

    @Override
    public void onCreate() {
        mContext = getApplicationContext();
        super.onCreate();
    }

    public static Context getContextOfApplication() {
        return mContext;
    }

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }

}