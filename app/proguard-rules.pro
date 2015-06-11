# android webview
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
    public *;
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
