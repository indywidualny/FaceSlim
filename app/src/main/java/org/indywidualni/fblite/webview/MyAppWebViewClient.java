package org.indywidualni.fblite.webview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.indywidualni.fblite.MyApplication;
import org.indywidualni.fblite.R;
import org.indywidualni.fblite.util.Connectivity;
import org.indywidualni.fblite.util.Dimension;
import org.indywidualni.fblite.util.FileOperation;

public class MyAppWebViewClient extends WebViewClient {

    // variable for onReceivedError
    private boolean refreshed;

    // get application context
    private static Context context = MyApplication.getContextOfApplication();

    // get shared preferences
    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

    // convert css file to string only one time
    private static String cssFile;

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // handling external links as intents
        if (Uri.parse(url).getHost().endsWith("facebook.com")
                || Uri.parse(url).getHost().endsWith("m.facebook.com")
                || Uri.parse(url).getHost().endsWith("h.facebook.com")
                || Uri.parse(url).getHost().endsWith("l.facebook.com")
                || Uri.parse(url).getHost().endsWith("0.facebook.com")
                || Uri.parse(url).getHost().endsWith("zero.facebook.com")
                || Uri.parse(url).getHost().endsWith("fb.me")) {
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        view.getContext().startActivity(intent);
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        // refresh on connection error (sometimes there is an error even when there is a network connection)
        if (!refreshed) {
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
        // when Zero is activated and there is a mobile network connection ignore extra customizations
        if (!preferences.getBoolean("facebook_zero", false) || !Connectivity.isConnectedMobile(context)) {
            // turn facebook black (experimental)
            if (preferences.getBoolean("dark_theme", false)) {
                // fill it with data only one time
                if (cssFile == null)
                    cssFile = FileOperation.readRawTextFile(context, R.raw.black);
                view.loadUrl("javascript:function addStyleString(str) { var node = document.createElement('style'); node.innerHTML = str; document.body.appendChild(node); } addStyleString('" + cssFile + "');");
            }
            // blue navigation bar always on top
            if (preferences.getBoolean("fixed_nav", false)) {
                String cssFixed = "#header{ position: fixed; z-index: 11; top: 0px; } #root{ padding-top: 44px; } .flyout{ max-height: " + Dimension.heightForFixedFacebookNavbar(context) + "px; overflow-y: scroll; }";
                if (Uri.parse(url).getHost().endsWith("mbasic.facebook.com"))
                    cssFixed = "#header{ position: fixed; z-index: 11; top: 0px; } #objects_container{ padding-top: 74px; }";
                view.loadUrl("javascript:function addStyleString(str) { var node = document.createElement('style'); node.innerHTML = str; document.body.appendChild(node); } addStyleString('" + cssFixed + "');");
            }
        }
        // apply extra bottom padding for transparent navigation
        if (preferences.getBoolean("transparent_nav", false))
            view.loadUrl("javascript:function addStyleString(str) { var node = document.createElement('style'); node.innerHTML = str; document.body.appendChild(node); } addStyleString('body{ padding-bottom: 48px; }');");

        // hide sponsored posts and ads (works only for an originally loaded section, not for a dynamically loaded content)
        if (preferences.getBoolean("hide_sponsored", false)) {
            final String cssHideSponsored = "#m_newsfeed_stream article[data-ft*=\"\\\"ei\\\":\\\"\"], .aymlCoverFlow, .aymlNewCoverFlow[data-ft*=\"\\\"is_sponsored\\\":\\\"1\\\"\"], .pyml, " +
                    ".storyStream > ._6t2[data-sigil=\"marea\"], .storyStream > .fullwidth._539p, .storyStream > article[id^=\"u_\"]._676, .storyStream > article[id^=\"u_\"].storyAggregation { display: none; }";
            view.loadUrl("javascript:function addStyleString(str) { var node = document.createElement('style'); node.innerHTML = str; document.body.appendChild(node); } addStyleString('" + cssHideSponsored + "');");
        }

        // hide news feed (a feature requested by drjedd)
        if (preferences.getBoolean("hide_news_feed", false))
            view.loadUrl("javascript:function addStyleString(str) { var node = document.createElement('style'); node.innerHTML = str; document.body.appendChild(node); } addStyleString('#m_newsfeed_stream{ display: none; }');");

        // hide people you may know
        if (preferences.getBoolean("hide_people", false))
            view.loadUrl("javascript:function addStyleString(str) { var node = document.createElement('style'); node.innerHTML = str; document.body.appendChild(node); } addStyleString('article._55wo._5rgr._5gh8._35au{ display: none; }');");

        // don't display images when they are disabled, we don't need empty placeholders
        if (preferences.getBoolean("no_images", false))
            view.loadUrl("javascript:function addStyleString(str) { var node = document.createElement('style'); node.innerHTML = str; document.body.appendChild(node); } addStyleString('.img, ._5s61, ._5sgg{ display: none; }');");
    }

}