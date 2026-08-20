package com.kriptobr.mercado.ui

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Aba "Painel": o site completo, para quem quer o raio-X da rede, as liquidações
 * e o conversor. É um lugar onde a pessoa entra quando quer — não a cara do app.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TelaPainel(aoCriarWebView: (WebView) -> Unit) {
    val ctx = LocalContext.current
    val web = remember {
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.userAgentString = settings.userAgentString + " KriptoBRApp/2.0"
            webViewClient = WebViewClient()
            setBackgroundColor(android.graphics.Color.parseColor("#091417"))
            loadUrl("https://mercado.kriptobr.com/?utm_source=app&utm_medium=android")
            aoCriarWebView(this)
        }
    }
    AndroidView(factory = { web }, modifier = Modifier.fillMaxSize())
}
