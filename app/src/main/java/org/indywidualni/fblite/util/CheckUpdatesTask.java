package org.indywidualni.fblite.util;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;

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
    private static final String NOTES = "https://github.com/indywidualny/FaceSlim/releases/latest";
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
            String[] parts = result.split(":");
            if (!parts[0].isEmpty() && !parts[1].isEmpty() && Integer.valueOf(parts[0]) > CURRENT_VERSION) {
                // there's a new version
                AppMsg appMsg = AppMsg.makeText(activity, activity.getString(R.string.new_version_detected)
                        + " (" + parts[1] + ")", new AppMsg.Style(AppMsg.LENGTH_LONG, R.color.colorAccent));
                appMsg.setLayoutGravity(Gravity.TOP);
                appMsg.getView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(NOTES)));
                    }
                });
                if (preferences.getBoolean("transparent_nav", false) && activity.getResources()
                        .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    appMsg.setLayoutParams(Dimension.getParamsAppMsg(activity));
                }
                appMsg.show();
            }
            if (!parts[0].isEmpty())
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
        int len = 25;
            
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
        // Example:  042:2.8.0
        Reader reader;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }

}