package org.indywidualni.fblite.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import net.grandcentrix.tray.TrayAppPreferences;

import org.indywidualni.fblite.MyApplication;
import org.indywidualni.fblite.util.database.OfflineDataSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Offline {

    private static String userAgent;
    private static Context context;
    private OfflineDataSource dataSource;

    private SharedPreferences preferences = PreferenceManager
            .getDefaultSharedPreferences(MyApplication.getContextOfApplication());

    public Offline() {
        context = MyApplication.getContextOfApplication();
        TrayAppPreferences trayPreferences = new TrayAppPreferences(context);
        userAgent = trayPreferences.getString("webview_user_agent", System.getProperty("http.agent"));
        dataSource = OfflineDataSource.getInstance();
        syncCookies();
    }

    public String getPage(String url) throws SQLException {
        url = removeRefID(url);
        url = removeEndingSlash(url);
        //Log.v(getClass().getSimpleName(), "Getting: " + url);
        return dataSource.getPage(url);
    }

    public void savePage(String url) throws SQLException {
        url = removeRefID(url);
        url = removeEndingSlash(url);
        //Log.v(getClass().getSimpleName(), "Saving: " + url);
        new SaveTask().execute(url);
    }

    public OfflineDataSource getDataSource() {
        return dataSource;
    }

    private class SaveTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... args) throws SQLException {
            try {
                final Document response = Jsoup.connect(args[0]).userAgent(userAgent)
                        .header("Accept-Encoding", "gzip, deflate").timeout(5000)
                        .cookie("https://m.facebook.com", CookieManager.getInstance().getCookie("https://m.facebook.com")).get();

                String base = "https://m.facebook.com";
                if (preferences.getBoolean("basic_mode", false))
                    base = "https://mbasic.facebook.com";
                if (Connectivity.isConnectedMobile(context) && preferences.getBoolean("facebook_zero", false))
                    base = "https://0.facebook.com";

                Document doc = Jsoup.parse(response.toString(), base);
                Elements select = doc.select("a");

                for (Element e : select) {
                    // baseUri will be used by absUrl
                    String absUrl = e.absUrl("href");
                    e.attr("href", absUrl);
                }

                // insert values into a database
                dataSource.insertPage(args[0], doc.toString());
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

    public static String removeRefID(String url) {
        url = url.replaceAll("\\?refid=.*", "");
        return url.replace("home.php", "");
    }

}