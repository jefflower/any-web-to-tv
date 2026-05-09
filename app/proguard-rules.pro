# Keep WebView JS interfaces (none currently, but reserved for future)
-keepclassmembers class com.jefflower.anywebtotv.web.* {
    @android.webkit.JavascriptInterface <methods>;
}

# GeckoView ships its own consumer-proguard rules in the AAR, but if R8 ever
# strips Gecko's JNI entry points the app crashes at startup. Belt + suspenders:
-keep class org.mozilla.gecko.** { *; }
-keep class org.mozilla.geckoview.** { *; }
-keep class mozilla.components.** { *; }
-dontwarn org.mozilla.**
