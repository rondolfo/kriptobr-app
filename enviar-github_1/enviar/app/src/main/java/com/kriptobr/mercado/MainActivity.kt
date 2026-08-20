package com.kriptobr.mercado

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kriptobr.mercado.alerta.Alerta
import com.kriptobr.mercado.alerta.VerificadorAlertas
import com.kriptobr.mercado.dados.*
import com.kriptobr.mercado.ui.*
import com.kriptobr.mercado.widget.WidgetCotacao
import kotlinx.coroutines.launch

private enum class Aba(val rotulo: Int) {
    MERCADO(R.string.aba_mercado),
    NOTICIAS(R.string.aba_noticias),
    ALERTAS(R.string.aba_alertas),
    PAINEL(R.string.aba_painel),
    LOJA(R.string.aba_loja)
}

class MainActivity : ComponentActivity() {

    private var webPainel: WebView? = null

    private val pedirNotificacao =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Avisos.criarCanais(this)
        VerificadorAlertas.agendar(this)
        pedirPermissaoNotificacao()

        setContent { TemaKriptoBR { Raiz() } }

        onBackPressedDispatcher.addCallback(this) {
            val w = webPainel
            if (w != null && w.canGoBack()) w.goBack() else finish()
        }
    }

    private fun pedirPermissaoNotificacao() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val ok = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (ok != PackageManager.PERMISSION_GRANTED) pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Composable
    private fun Raiz() {
        val ctx = LocalContext.current
        var abaIndice by rememberSaveable { mutableIntStateOf(0) }
        val aba = Aba.entries[abaIndice.coerceIn(0, Aba.entries.lastIndex)]
        var mercado by remember { mutableStateOf(Repo.ultimoConhecido(ctx)) }
        var carregando by remember { mutableStateOf(true) }
        var erro by remember { mutableStateOf<String?>(null) }
        var alertas by remember { mutableStateOf(Guardados.alertas(ctx)) }
        var favoritos by remember { mutableStateOf(Guardados.favoritos(ctx)) }
        var editando by remember { mutableStateOf(false) }
        var catalogo by remember { mutableStateOf<List<Moeda>>(emptyList()) }
        var noticias by remember { mutableStateOf<List<Noticia>>(emptyList()) }
        var carregandoNoticias by remember { mutableStateOf(true) }
        var erroNoticias by remember { mutableStateOf<String?>(null) }
        // O corte fica congelado na abertura do app: depois de marcar como vistas,
        // os pontinhos verdes continuam na tela até a pessoa sair — some só na próxima vez.
        val corteNovas = remember { Noticias.visto(ctx) }

        val buscarNoticias: () -> Unit = {
            carregandoNoticias = true
            lifecycleScope.launch {
                runCatching { Noticias.ultimas() }
                    .onSuccess { noticias = it; erroNoticias = null }
                    .onFailure { erroNoticias = getString(R.string.noticias_vazio) }
                carregandoNoticias = false
            }
        }

        LaunchedEffect(Unit) {
            buscarNoticias()
            Miniaturas.limparAntigas(ctx)      // suspende e roda fora da thread da tela
        }

        val novas = noticias.count { corteNovas > 0L && it.quando > corteNovas }

        // marca como vistas quando a pessoa realmente entra na aba
        LaunchedEffect(aba, noticias) {
            if (aba == Aba.NOTICIAS && noticias.isNotEmpty()) {
                kotlinx.coroutines.delay(1200)
                Noticias.marcarVisto(ctx, noticias.maxOf { it.quando })
            }
        }

        val atualizar: () -> Unit = {
            carregando = true
            lifecycleScope.launch {
                Repo.carregar(ctx)
                    .onSuccess { mercado = it; erro = null }
                    .onFailure { erro = getString(R.string.sem_rede) }
                carregando = false
                WidgetCotacao.redesenharTodos(ctx)
            }
        }

        LaunchedEffect(favoritos) { atualizar() }

        if (editando) {
            LaunchedEffect(Unit) {
                if (catalogo.isEmpty()) {
                    runCatching { Api.catalogo(Guardados.fiat()) }.onSuccess { catalogo = it }
                }
            }
            TelaEditarLista(
                catalogo = catalogo,
                favoritos = favoritos,
                carregando = catalogo.isEmpty(),
                aoAlternar = { id -> favoritos = Guardados.alternarFavorito(ctx, id) },
                aoFechar = { editando = false }
            )
            return
        }

        Scaffold(
            containerColor = Fundo,
            topBar = {
                BarraTopo(
                    aba = aba,
                    carregando = if (aba == Aba.NOTICIAS) carregandoNoticias else carregando,
                    aoAtualizar = if (aba == Aba.NOTICIAS) buscarNoticias else atualizar
                )
            },
            bottomBar = { BarraAbas(aba, novas) { abaIndice = it.ordinal } }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                when (aba) {
                    Aba.MERCADO -> TelaMercado(
                        mercado = mercado,
                        carregando = carregando,
                        erro = erro,
                        aoEditarLista = { editando = true },
                        aoTocarMoeda = { }
                    )
                    Aba.NOTICIAS -> TelaNoticias(
                        noticias = noticias,
                        carregando = carregandoNoticias,
                        erro = erroNoticias,
                        corteNovas = corteNovas,
                        aoAbrir = { abrirNoticia(ctx, it) }
                    )
                    Aba.ALERTAS -> TelaAlertas(
                        mercado = mercado,
                        alertas = alertas,
                        aoCriar = { novo ->
                            alertas = (alertas + novo).also { Guardados.salvarAlertas(ctx, it) }
                        },
                        aoAlternar = { alvo ->
                            alertas = alertas.map {
                                if (it.id == alvo.id) it.copy(ativo = !it.ativo, disparadoEm = 0L) else it
                            }.also { Guardados.salvarAlertas(ctx, it) }
                        },
                        aoRemover = { alvo ->
                            alertas = alertas.filterNot { it.id == alvo.id }
                                .also { Guardados.salvarAlertas(ctx, it) }
                        }
                    )
                    Aba.PAINEL -> TelaPainel { webPainel = it }
                    Aba.LOJA -> TelaLoja()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BarraTopo(aba: Aba, carregando: Boolean, aoAtualizar: () -> Unit) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(26.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Mint),
                        contentAlignment = Alignment.Center
                    ) { Text("\u20BF", color = MintTinta, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
                    Spacer(Modifier.width(9.dp))
                    Text("KriptoBR", color = Tinta, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(aba.rotulo), color = Apagado, fontSize = 13.sp)
                }
            },
            actions = {
                if (aba == Aba.MERCADO || aba == Aba.NOTICIAS) {
                    if (carregando) {
                        CircularProgressIndicator(
                            color = Mint, strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp).padding(end = 2.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                    } else {
                        IconButton(onClick = aoAtualizar) {
                            Icon(Icons.Filled.Refresh, stringResource(R.string.atualizar), tint = Tinta2)
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Fundo)
        )
    }

    /* O ícone fica aqui, e não no enum, porque um deles é um desenho nosso
       (jornal) e os outros vêm do conjunto do Material. */
    @Composable
    private fun IconeAba(a: Aba) {
        when (a) {
            Aba.MERCADO -> Icon(Icons.Filled.Home, null)
            Aba.NOTICIAS -> Icon(painterResource(R.drawable.ic_noticias), null, Modifier.size(22.dp))
            Aba.ALERTAS -> Icon(Icons.Filled.Notifications, null)
            Aba.PAINEL -> Icon(Icons.Filled.Menu, null)
            Aba.LOJA -> Icon(Icons.Filled.Star, null)
        }
    }

    @Composable
    private fun BarraAbas(atual: Aba, novasNoticias: Int, aoTrocar: (Aba) -> Unit) {
        NavigationBar(containerColor = Superficie, tonalElevation = 0.dp) {
            Aba.entries.forEach { a ->
                NavigationBarItem(
                    selected = a == atual,
                    onClick = { aoTrocar(a) },
                    icon = {
                        Box {
                            IconeAba(a)
                            if (a == Aba.NOTICIAS && novasNoticias > 0) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 5.dp, y = (-3).dp)
                                        .size(8.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Mint)
                                )
                            }
                        }
                    },
                    label = { Text(stringResource(a.rotulo), fontSize = 10.5f.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Mint, selectedTextColor = Mint,
                        unselectedIconColor = Apagado, unselectedTextColor = Apagado,
                        indicatorColor = Superficie2
                    )
                )
            }
        }
    }
}
