# android webview
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
    public *;
}

# image upload
-keepclassmembers class * {
    public void openFileChooser(android.webkit.ValueCallback, java.lang.String);
    public void openFileChooser(android.webkit.ValueCallback);
    public void openFileChooser(android.webkit.ValueCallback, java.lang.String, java.lang.String);
    public boolean onShowFileChooser(android.webkit.WebView, android.webkit.ValueCallback, android.webkit.WebChromeClient.FileChooserParams);
}

# jsoup library
-keeppackagenames org.jsoup.nodes
