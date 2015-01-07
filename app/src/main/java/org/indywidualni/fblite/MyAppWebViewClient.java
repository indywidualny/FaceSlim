package org.indywidualni.fblite;

// handling external links as intents

import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MyAppWebViewClient extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if( Uri.parse(url).getHost().endsWith("m.facebook.com") || Uri.parse(url).getHost().endsWith("h.facebook.com") || Uri.parse(url).getHost().endsWith("l.facebook.com") ) {
            return false;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        view.getContext().startActivity(intent);
        return true;
    }
}