package org.indywidualni.fblite.webview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.SQLException;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ListView;

import com.devspark.appmsg.AppMsg;

import org.indywidualni.fblite.MyApplication;
import org.indywidualni.fblite.R;
import org.indywidualni.fblite.activity.MainActivity;
import org.indywidualni.fblite.util.Connectivity;
import org.indywidualni.fblite.util.Dimension;
import org.indywidualni.fblite.util.FileOperation;
import org.indywidualni.fblite.util.FloatingActionButton;
import org.indywidualni.fblite.util.Miscellany;
import org.indywidualni.fblite.util.Offline;
import org.indywidualni.fblite.util.OfflinePagesAdapter;

import java.util.ArrayList;

public class MyWebViewClient extends WebViewClient {

    // a currently loaded page
    public static String currentlyLoadedPage;
    private static long lastSavingTime = System.currentTimeMillis();
    public static boolean wasOffline;

    // variable for onReceivedError
    private boolean refreshed;

    // get application context
    private static final Context context = MyApplication.getContextOfApplication();

    // get shared preferences
    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

    // convert css file to string only one time
    private static String cssFile;

    // object for offline mode management
    private Offline offline;
    private FloatingActionButton fab;
    private static WebView webView;

    // in order to load pages we need a reference to a WebView object from MainActivity
    public static void setWebviewReference(WebView wv) {
        webView = wv;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return shouldOverrideUrlLoading(view, request.getUrl().toString());
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // clean an url from facebook redirection before processing (no more blank pages on back)
        if (url != null)
            url = Miscellany.cleanAndDecodeUrl(url);
        // really ugly but let's do it just to avoid rare crashes
        try {
            if (Uri.parse(url).getHost().endsWith("messenger.com")) {
                view.getSettings().setUseWideViewPort(false);
                return false;
            } else
                view.getSettings().setUseWideViewPort(true);

            if (Uri.parse(url).getHost().endsWith("facebook.com")
                    || Uri.parse(url).getHost().endsWith("mobile.facebook.com")
                    || Uri.parse(url).getHost().endsWith("touch.facebook.com")
                    || Uri.parse(url).getHost().endsWith("m.facebook.com")
                    || Uri.parse(url).getHost().endsWith("h.facebook.com")
                    || Uri.parse(url).getHost().endsWith("l.facebook.com")
                    || Uri.parse(url).getHost().endsWith("0.facebook.com")
                    || Uri.parse(url).getHost().endsWith("zero.facebook.com")
                    || Uri.parse(url).getHost().endsWith("fbcdn.net")
                    || Uri.parse(url).getHost().endsWith("akamaihd.net")
                    || Uri.parse(url).getHost().endsWith("fb.me")) {
                return false;
            } else if (preferences.getBoolean("load_extra", false)
                    && (Uri.parse(url).getHost().endsWith("googleusercontent.com")
                    || Uri.parse(url).getHost().endsWith("tumblr.com")
                    || Uri.parse(url).getHost().endsWith("pinimg.com")
                    || Uri.parse(url).getHost().endsWith("media.giphy.com"))) {
                return false;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                view.getContext().startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e("shouldOverrideUrlLoad", "No Activity to handle action", e);
                e.printStackTrace();
            }
            return true;
        } catch (NullPointerException npe) {
            npe.printStackTrace();
            return true;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        // refresh on connection error (sometimes there is an error even when there is a network connection)
        if (Connectivity.isConnected(context) && !failingUrl.contains("edge-chat") && !failingUrl.contains("akamaihd")
                && !failingUrl.contains("atdmt") && !refreshed) {
            view.loadUrl(failingUrl);
            // when network error is real do not reload url again
            refreshed = true;
        }
    }

    @TargetApi(android.os.Build.VERSION_CODES.M)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
        // redirect to deprecated method, so we can use it in all SDK versions
        onReceivedError(view, err.getErrorCode(), err.getDescription().toString(), req.getUrl().toString());
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        if (url.contains("messenger.com"))
            ((MainActivity) MainActivity.getMainActivity()).getSwipeRefreshLayout().setEnabled(false);
        else
            ((MainActivity) MainActivity.getMainActivity()).getSwipeRefreshLayout().setEnabled(true);

        // when Zero is activated and there is a mobile network connection ignore extra customizations
        if (!preferences.getBoolean("facebook_zero", false) || !Connectivity.isConnectedMobile(context)) {

            // hide install messenger notice by default
            view.loadUrl("javascript:function addStyleString(str) { var node = document.createElement('style'); " +
                    "node.innerHTML = str; document.body.appendChild(node); } " +
                    "addStyleString('[data-sigil*=m-promo-jewel-header]{ display: none; }');");

            // turn facebook black (experimental)
            if (preferences.getBoolean("dark_theme", false)) {
                // fill it with data only one time
                if (cssFile == null)
                    cssFile = FileOperation.readRawTextFile(context, R.raw.black);
                view.loadUrl("javascript:function addStyleString(str) { var node = document.createElement('style'); " +
                        "node.innerHTML = str; document.body.appendChild(node); } addStyleString('" + cssFile + "');");
            }
        }

        // blue navigation bar always on top
        if (preferences.getBoolean("fixed_nav", false)) {
            String cssFixed = "#header{ position: fixed; z-index: 11; top: 0px; } #root{ padding-top: 44px; } " +
                    ".flyout{ max-height: " + Dimension.heightForFixedFacebookNavbar(context) + "px; overflow-y: scroll; }";

            // there is no host in offline mode
            if (Uri.parse(url).getHost() != null) {
                if (Uri.parse(url).getHost().endsWith("mbasic.facebook.com"))
                    cssFixed = "#header{ position: fixed; z-index: 11; top: 0px; } #objects_container{ padding-top: 74px; }";
                else if (Uri.parse(url).getHost().endsWith("0.facebook.com"))
                    cssFixed = "#toggleHeaderContent{position: fixed; z-index: 11; top: 0px;} " +
                            "#header{ position: fixed; z-index: 11; top: 28px; } #objects_container{ padding-top: 102px; }";
            }

            view.loadUrl("javascript:function addStyleString(str) { var node = document.createElement('style'); " +
                    "node.innerHTML = str; document.body.appendChild(node); } addStyleString('" + cssFixed + "');");
        }

        // apply extra bottom padding for transparent navigation
        if (preferences.getBoolean("transparent_nav", false))
            view.loadUrl("javascript:function addStyleString(str) { var node = document.createElement('style'); " +
                    "node.innerHTML = str; document.body.appendChild(node); } addStyleString('body{ padding-bottom: 48px; }');");

        // hide sponsored posts and ads (works only for an originally loaded section, not for a dynamically loaded content)
        if (preferences.getBoolean("hide_sponsored", false)) {
            final String cssHideSponsored = "#m_newsfeed_stream article[data-ft*=\"\\\"ei\\\":\\\"\"], " +
                    ".aymlCoverFlow, .aymlNewCoverFlow[data-ft*=\"\\\"is_sponsored\\\":\\\"1\\\"\"], .pyml, " +
                    ".storyStream > ._6t2[data-sigil=\"marea\"], .storyStream > .fullwidth._539p, .storyStream > " +
                    "article[id^=\"u_\"]._676, .storyStream > article[id^=\"u_\"].storyAggregation { display: none; }";
            view.loadUrl("javascript:function addStyleString(str) { var node = document.createElement('style'); " +
                    "node.innerHTML = str; document.body.appendChild(node); } addStyleString('" + cssHideSponsored + "');");
        }

        // hide people you may know
        if (preferences.getBoolean("hide_people", false))
            view.loadUrl("javascript:function addStyleString(str) { var node = document.createElement('style'); " +
                    "node.innerHTML = str; document.body.appendChild(node); } " +
                    "addStyleString('article._55wo._5rgr._5gh8._35au{ display: none; }');");

        // hide news feed (a feature requested by drjedd)
        if (preferences.getBoolean("hide_news_feed", false))
            view.loadUrl("javascript:function addStyleString(str) { var node = document.createElement('style'); " +
                    "node.innerHTML = str; document.body.appendChild(node); } " +
                    "addStyleString('#m_newsfeed_stream{ display: none; }');");

        // don't display images when they are disabled, we don't need empty placeholders
        if (preferences.getBoolean("no_images", false))
            view.loadUrl("javascript:function addStyleString(str) { var node = document.createElement('style'); " +
                    "node.innerHTML = str; document.body.appendChild(node); } " +
                    "addStyleString('.img, ._5sgg, ._-_a, .widePic, .profile-icon{ display: none; }');");

        // hide install messenger notice at messenger page
        if (url.contains("messenger.com")) {
            view.loadUrl("javascript:function addStyleString(str) { var node = document.createElement('style'); " +
                    "node.innerHTML = str; document.body.appendChild(node); } " +
                    "addStyleString('._s15{ display: none; }');");
        }

        // offline mode
        if (preferences.getBoolean("offline_mode", false)) {
            if (offline == null)
                offline = new Offline();

            // hide floating action button
            if (fab != null && Connectivity.isConnected(context) && !fab.isHidden())
                fab.hideFloatingActionButton();

            // reset offline status, it's gonna be set later if needed
            if (Connectivity.isConnected(context))
                wasOffline = false;

            // rare case when we suddenly got an internet connection but an offline page is being refreshed
            if (Connectivity.isConnected(context) && Uri.parse(url).getHost() == null) {
                wasOffline = false;  // again, just to be 100% sure
                webView.loadUrl(currentlyLoadedPage);
                return;
            }

            // is url valid and is it a facebook page
            if (!URLUtil.isValidUrl(url) || !url.contains("facebook.com"))
                return;

            try {
                if (Connectivity.isConnected(context)) {
                    // save the current page when online
                    if (!currentlyLoadedPage.equals(url)) {
                        offline.savePage(url);
                        lastSavingTime = System.currentTimeMillis();
                    } else if (currentlyLoadedPage.equals(url) && lastSavingTime < System.currentTimeMillis() - 5500) {
                        // don't save it when it's the same page which was saved less than 5.5s ago
                        offline.savePage(url);
                        lastSavingTime = System.currentTimeMillis();
                    }
                } else {
                    // try to load page from the database when offline
                    view.loadData(offline.getPage(url), "text/html; charset=utf-8", "UTF-8");
                    wasOffline = true;

                    // show the message at the bottom of the screen
                    AppMsg appMsg = AppMsg.makeText(MainActivity.getMainActivity(),
                            context.getString(R.string.loading_offline_database),
                            new AppMsg.Style(AppMsg.LENGTH_SHORT, R.color.colorAccent));
                    appMsg.setLayoutGravity(Gravity.TOP);
                    if (preferences.getBoolean("transparent_nav", false) && context.getResources()
                            .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                        appMsg.setLayoutParams(Dimension.getParamsAppMsg(context));
                    appMsg.show();

                    // floating action button
                    if (fab == null) {
                        fab = new FloatingActionButton.Builder(MainActivity.getMainActivity())
                                .withDrawable(ContextCompat.getDrawable(context, R.mipmap.ic_device_storage))
                                .withButtonColor(ContextCompat.getColor(context, R.color.colorAccent))
                                .withGravity(Gravity.BOTTOM | Gravity.END)
                                .withMargins(0, 0, 16, 16)
                                .create();

                        if (preferences.getBoolean("transparent_nav", false))
                            fab.setY(-Dimension.getNavigationBarHeight(context, 0));

                        // on click listener for fab
                        fab.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (offline == null) {
                                    fab.hideFloatingActionButton();
                                    return;
                                }

                                LayoutInflater inflater = MainActivity.getMainActivity().getLayoutInflater();
                                @SuppressLint("InflateParams") View customView = inflater
                                        .inflate(R.layout.dialog_offline_list, null);

                                final Dialog dialog = new Dialog(MainActivity.getMainActivity(), R.style.CustomDialogTheme);
                                dialog.setContentView(customView);
                                dialog.show();

                                final ArrayList<String> page = offline.getDataSource().getAllPages();
                                ListView lv = (ListView) dialog.findViewById(R.id.list_offline);
                                OfflinePagesAdapter opa = new OfflinePagesAdapter(MainActivity.getMainActivity(), page);
                                lv.setEmptyView(dialog.findViewById(R.id.empty_element));
                                lv.setAdapter(opa);

                                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        webView.loadUrl(page.get(position));
                                        dialog.dismiss();
                                    }
                                });
                            }
                        });
                    } else if (fab.isHidden())
                        fab.showFloatingActionButton();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else
            offline = null;

        // save the currently loaded page (needed for a reliable refreshing)
        currentlyLoadedPage = url;
    }

}
