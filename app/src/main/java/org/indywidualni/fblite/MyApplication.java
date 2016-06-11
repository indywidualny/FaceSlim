package org.indywidualni.fblite;

import android.app.Application;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.indywidualni.fblite.util.Connectivity;
import org.piwik.sdk.Piwik;
import org.piwik.sdk.Tracker;

import java.net.MalformedURLException;

@ReportsCrashes(formUri = "",  // will not be used
        mailTo = "koras@indywidualni.org",
        mode = ReportingInteractionMode.DIALOG,
        resToastText = R.string.crash_toast_text,
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = R.mipmap.ic_launcher,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
        resDialogTheme = R.style.CrashDialog
        )

public class MyApplication extends Application {

    private static Context mContext;
    private Tracker mPiwikTracker;

    @Override
    public void onCreate() {
        mContext = getApplicationContext();
        super.onCreate();

        /**
         * The following line triggers the initialization of ACRA.
         */
        ACRA.init(this);

        /**
         * Piwik dry run. Uncomment these lines during app development.
         */
        Piwik.getInstance(this).setDryRun(BuildConfig.DEBUG);
        Piwik.getInstance(this).setDebug(BuildConfig.DEBUG);

        /**
         * Count app downloads. Fired only after new installation or upgrade.
         * It's never fired again. In fact the app is not tracking anything but installations.
         */
        if (Connectivity.isConnected(this)) {
            getTracker().trackAppDownload();
            getTracker().dispatch();
        }
    }

    /**
     * Get context of application for non-context classes
     * @return context of application
     */
    public static Context getContextOfApplication() {
        return mContext;
    }

    /**
     * Get Piwik tracker. No sensitive data is collected. Just app version, predicted location,
     * resolution, device model and system version. Location is based on anonymized IP address.
     * @return tracker instance
     */
    public synchronized Tracker getTracker() {
        if (mPiwikTracker != null)
            return mPiwikTracker;

        try {
            mPiwikTracker = Piwik.getInstance(this).newTracker("http://indywidualni.org/analytics/piwik.php", 1);
            mPiwikTracker.setUserId(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
            mPiwikTracker.setDispatchTimeout(30000);
            mPiwikTracker.setDispatchInterval(-1);
        } catch (MalformedURLException e) {
            Log.w("Piwik", "url is malformed", e);
            return null;
        }

        return mPiwikTracker;
    }

}