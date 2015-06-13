# android webview
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
    public *;
}

# image upload
-keepclassmembers class * {
    public void openFileChooser(android.webkit.ValueCallback, java.lang.String);
    public void openFileChooser(android.webkit.ValueCallback);
    public void openFileChooser(android.webkit.ValueCallback, java.lang.String, java.lang.String);
}

# saxrssreader library
-keep class nl.matshofman.saxrssreader.** {
    *;
}

# remove logs (disabled for now)
# works only with proguard-android-optimize.txt
-assumenosideeffects class android.util.Log {
    *;
}
