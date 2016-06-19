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

import org.indywidualni.fblite.MyApplication;
import org.indywidualni.fblite.R;
import org.indywidualni.fblite.util.database.OfflineDataSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Offline {

    private static String userAgent;
    private static Context context;
    private final OfflineDataSource dataSource;

    private final SharedPreferences preferences = PreferenceManager
            .getDefaultSharedPreferences(MyApplication.getContextOfApplication());

    public Offline() {
        context = MyApplication.getContextOfApplication();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        userAgent = preferences.getString("webview_user_agent", System.getProperty("http.agent"));
        if (!preferences.getString("custom_user_agent", context.getString(R.string.predefined_user_agent)).isEmpty())
            userAgent = preferences.getString("custom_user_agent", context.getString(R.string.predefined_user_agent));
        dataSource = OfflineDataSource.getInstance();
        syncCookies();
    }

    public String getPage(String url) throws SQLException {
        url = cleanUrl(url);
        //Log.v(getClass().getSimpleName(), "Getting: " + url);
        return dataSource.getPage(url);
    }

    public void savePage(String url) throws SQLException {
        url = cleanUrl(url);
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
                if (preferences.getBoolean("touch_mode", false))
                    base = "https://touch.facebook.com";
                else if (preferences.getBoolean("basic_mode", false))
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

    private static String removeEndingSlash(String url) {
        if (url.length() > 0 && url.charAt(url.length()-1)=='/')
            url = url.substring(0, url.length()-1);
        return url;
    }

    public static String cleanUrl(String url) {
        /**
         * remove referrers     all the variations of referrers
         * remove home.php      it's always first and nothing is later (main page)
         * replace              mobile.  to  m.
         * remove ?_rdr         it's always last and nothing is later
         * remove &_rdr         it's always last and nothing is later
         * remove /             it's always last and nothing is later
         */
        url = url.replaceAll("\\?refid=.*", "").replaceAll("&refid=.*", "").replaceAll("\\?hrc=.*", "")
                .replaceAll("&hrc=.*", "").replaceAll("\\?refsrc=.*", "").replaceAll("&refsrc=.*", "")
                .replaceAll("\\?fref=.*", "").replaceAll("&fref=.*", "").replaceAll("\\?ref=.*", "")
                .replaceAll("&ref=.*", "").replaceAll("\\?ref_type=.*", "").replaceAll("&ref_type=.*", "")
                .replaceAll("\\?ref_component=.*", "").replaceAll("&ref_component=.*", "")
                .replaceAll("\\?ref_page=.*", "").replaceAll("&ref_page=.*", "")
                .replaceAll("\\?fb_ref=.*", "").replaceAll("&fb_ref=.*", "");
        url = url.replace("home.php", "").replace("mobile.", "m.").replace("?_rdr", "").replace("&_rdr", "");
        return removeEndingSlash(url);
    }

}