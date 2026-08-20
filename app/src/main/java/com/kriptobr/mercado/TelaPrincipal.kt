package com.kriptobr.mercado

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * Casca do aplicativo: mostra o painel mercado.kriptobr.com numa WebView.
 * Links de fora dos domínios da KriptoBR abrem no navegador do sistema, para o
 * visitante nunca ficar preso dentro do app sem saber onde está.
 */
class TelaPrincipal : AppCompatActivity() {

    private lateinit var web: WebView
    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var offline: View

    private val dominiosInternos = listOf(
        "mercado.kriptobr.com", "kriptobr.com", "www.kriptobr.com",
        "kriptohoje.com", "www.kriptohoje.com"
    )

    private val pedirNotificacao =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tela_principal)

        web = findViewById(R.id.web)
        refresh = findViewById(R.id.refresh)
        offline = findViewById(R.id.offline)

        refresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.mint))
        refresh.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(this, R.color.superficie))
        refresh.setOnRefreshListener { web.reload() }

        findViewById<Button>(R.id.btnTentarDeNovo).setOnClickListener {
            offline.visibility = View.GONE
            web.visibility = View.VISIBLE
            web.loadUrl(URL_INICIAL)
        }

        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            // deixa claro na análise de acesso que a visita veio do aplicativo
            userAgentString = "$userAgentString KriptoBRApp/${BuildConfig.VERSION_NAME}"
        }
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, req: WebResourceRequest): Boolean {
                val host = req.url.host ?: return false
                if (dominiosInternos.any { host == it || host.endsWith(".$it") }) return false
                abrirNoNavegador(req.url)
                return true
            }

            override fun onPageStarted(v: WebView?, url: String?, favicon: Bitmap?) {
                offline.visibility = View.GONE
            }

            override fun onPageFinished(v: WebView?, url: String?) {
                refresh.isRefreshing = false
            }

            override fun onReceivedError(v: WebView, req: WebResourceRequest, err: WebResourceError) {
                if (!req.isForMainFrame) return          // imagem ou API que falhou não derruba a tela
                refresh.isRefreshing = false
                web.visibility = View.GONE
                offline.visibility = View.VISIBLE
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) = request.deny()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) web.goBack() else finish()
            }
        })

        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl(intent?.data?.toString() ?: URL_INICIAL)

        Avisos.criarCanal(this)
        pedirPermissaoNotificacao()
    }

    private fun pedirPermissaoNotificacao() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val ok = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (ok != PackageManager.PERMISSION_GRANTED) pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun abrirNoNavegador(uri: Uri) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { web.loadUrl(it.toString()) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }

    companion object {
        const val URL_INICIAL = "https://mercado.kriptobr.com/?utm_source=app&utm_medium=android"
    }
}
