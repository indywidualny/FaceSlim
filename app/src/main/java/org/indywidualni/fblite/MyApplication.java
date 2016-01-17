package org.indywidualni.fblite;

import android.app.Application;
import android.content.Context;

import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(formUri = "",  // will not be used
        mailTo = "koras@indywidualni.org",
        mode = ReportingInteractionMode.DIALOG,
        resToastText = R.string.crash_toast_text,
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = R.mipmap.ic_launcher,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt
        )

public class MyApplication extends Application {

    // context of application for non context classes
    private static Context mContext;

    @Override
    public void onCreate() {
        mContext = getApplicationContext();
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }

    public static Context getContextOfApplication() {
        return mContext;
    }

}