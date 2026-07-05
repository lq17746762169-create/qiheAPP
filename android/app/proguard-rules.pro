# 契合 App ProGuard 规则
# WebView 应用一般不需要混淆优化

-keepattributes *Annotation*

# 保留 JavaScript 接口方法
-keepclassmembers class com.qihe.app.MainActivity$AndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# 保留 AndroidX WebView
-keep class androidx.webkit.** { *; }
