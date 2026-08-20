# Mantém o que a WebView e o widget precisam por reflexão
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.kriptobr.mercado.** { *; }
