package org.indywidualni.fblite.util;

import android.content.Context;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import net.grandcentrix.tray.TrayAppPreferences;

import org.indywidualni.fblite.MyApplication;
import org.indywidualni.fblite.util.database.OfflineDataSource;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Offline {

    private static String userAgent;
    private static Context context;
    private OfflineDataSource dataSource;

    public Offline() {
        context = MyApplication.getContextOfApplication();
        TrayAppPreferences trayPreferences = new TrayAppPreferences(context);
        userAgent = trayPreferences.getString("webview_user_agent", System.getProperty("http.agent"));
        dataSource = OfflineDataSource.getInstance();
        syncCookies();
    }

    public String getPage(String url) throws SQLException {
        url = removeEndingSlash(url);
        Log.v(getClass().getSimpleName(), "Getting: " + url);
        return dataSource.getPage(url);
    }

    public void savePage(String url) throws SQLException {
        url = removeEndingSlash(url);
        Log.v(getClass().getSimpleName(), "Saving: " + url);
        new SaveTask().execute(url);
    }

    // for debugging
    public OfflineDataSource getDataSource() {
        return dataSource;
    }

    private class SaveTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... args) throws SQLException {
            try {
                final Connection.Response response = Jsoup.connect(args[0]).userAgent(userAgent)
                        .cookie("https://m.facebook.com", CookieManager.getInstance().getCookie("https://m.facebook.com")).execute();
                final Document doc = response.parse();
                dataSource.insertPage(args[0], doc.outerHtml());
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "Problem saving the current page");
                e.printStackTrace();
            }
            return null;
        }

    }

    /** CookieSyncManager was deprecated in API level 21.
     *  We need it for API level lower than 21 though.
     *  In API level >= 21 it's done automatically.
     */
    @SuppressWarnings("deprecation")
    private void syncCookies() {
        if (Build.VERSION.SDK_INT < 21) {
            CookieSyncManager.createInstance(context);
            CookieSyncManager.getInstance().sync();
        }
    }

    public static String removeEndingSlash(String url) {
        if (url.length() > 0 && url.charAt(url.length()-1)=='/')
            url = url.substring(0, url.length()-1);
        return url;
    }

}