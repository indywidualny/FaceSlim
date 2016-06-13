package org.indywidualni.fblite.util;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;

import com.devspark.appmsg.AppMsg;

import org.indywidualni.fblite.BuildConfig;
import org.indywidualni.fblite.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CheckUpdatesTask extends AsyncTask<Void, Void, String> {

    public static final String TAG = "CheckUpdatesTask";
    private static final String URL = "https://raw.githubusercontent.com/indywidualny/FaceSlim/master/VERSION";
    private static final int CURRENT_VERSION = BuildConfig.VERSION_CODE;

    private Activity activity;
    private SharedPreferences preferences;

    public CheckUpdatesTask(Activity activity) {
        this.activity = activity;
        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    @Override
    protected String doInBackground(Void... ignored) {
        try {
            return downloadUrl(URL);
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    protected void onPostExecute(String result) {
        try {
            result = result.trim();
            if (!result.isEmpty() && Integer.valueOf(result) > CURRENT_VERSION) {
                // there's a new version
                AppMsg appMsg = AppMsg.makeText(activity, activity.getString(R.string.new_version_detected),
                        new AppMsg.Style(AppMsg.LENGTH_LONG, R.color.colorAccent));
                appMsg.setLayoutGravity(Gravity.TOP);
                if (preferences.getBoolean("transparent_nav", false) && activity.getResources()
                        .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    appMsg.setLayoutParams(Dimension.getParamsAppMsg(activity));
                }
                appMsg.show();
            }
            if (!result.isEmpty())
                preferences.edit().putLong("latest_update_check", System.currentTimeMillis()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Sad face... Error", e);
        } finally {
            activity = null;
            preferences = null;
        }
    }

    private String downloadUrl(String myUrl) throws IOException {
        InputStream is = null;
        int len = 3;
            
        try {
            URL url = new URL(myUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(TAG, "The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string
            return readIt(is, len);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private String readIt(InputStream stream, int len) throws IOException {
        Reader reader;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }

}