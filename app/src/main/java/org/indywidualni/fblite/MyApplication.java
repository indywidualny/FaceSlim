package org.indywidualni.fblite;

import android.app.Application;
import android.content.Context;

public class MyApplication extends Application {

    // context of application for non context classes
    private static Context mContext;

    @Override
    public void onCreate() {
        mContext = getApplicationContext();
        super.onCreate();
    }

    public static Context getContextOfApplication() {
        return mContext;
    }

}