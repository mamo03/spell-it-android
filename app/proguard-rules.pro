# Keep the JS-to-native TTS bridge methods reachable from WebView's JavaScript engine.
-keepclassmembers class com.spellit.kids.MainActivity$TtsBridge {
    public *;
}
