package org.indywidualni.fblite;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.webkit.WebView;

public class MainActivity extends Activity {

    SwipeRefreshLayout swipeRefreshLayout;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_container);
        swipeRefreshLayout.setOnRefreshListener(onRefreshListener);
        swipeRefreshLayout.setColorSchemeColors(Color.BLUE);

        // webView code without handling external links
        webView = (WebView) findViewById(R.id.webView1);
        webView.getSettings().setJavaScriptEnabled(true);

        // loads a page as Facebook says, not on its own way
        //webView.getSettings().setUseWideViewPort(true);
        //webView.getSettings().setLoadWithOverviewMode(true);

        webView.setWebViewClient(new MyAppWebViewClient());
        webView.loadUrl("https://m.facebook.com");
    }

    SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {

    @Override
    public void onRefresh() {
        // reloading page: two ways of doing it
        //webView.loadUrl( "javascript:window.location.reload( true )" );
        webView.reload();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(false);
                // done!
            }

        }, 2000);
    }};

    // survive screen orientation change
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // handling back button
    @Override
    public void onBackPressed() {
        if(webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

}