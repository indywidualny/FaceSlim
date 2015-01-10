package org.indywidualni.fblite;

import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MyAppWebViewClient extends WebViewClient {

    // handling external links as intents
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {

        if( Uri.parse(url).getHost().endsWith("m.facebook.com") || Uri.parse(url).getHost().endsWith("h.facebook.com") || Uri.parse(url).getHost().endsWith("l.facebook.com") || Uri.parse(url).getHost().endsWith("0.facebook.com") || Uri.parse(url).getHost().endsWith("zero.facebook.com") ) {
            return false;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        view.getContext().startActivity(intent);
        return true;
    }

}