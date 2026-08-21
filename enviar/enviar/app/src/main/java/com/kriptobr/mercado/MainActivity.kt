package com.kriptobr.mercado

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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

    /* É aqui que o Android decide de qual pasta values-* vão sair as frases.
       Sem isto, escolher "Português" com o celular em inglês não mudaria nada. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Idioma.envolver(newBase))
    }

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

        /* Duas buscas podem estar no ar ao mesmo tempo (a da abertura e a do
           botão de atualizar). Sem este contador, a resposta antiga chegando
           depois da nova reacendia o aviso de "sem conexão" com os preços já
           certos na tela — foi exatamente o que apareceu no print. */
        val pedidos = remember { java.util.concurrent.atomic.AtomicInteger(0) }
        val atualizar: () -> Unit = {
            val meu = pedidos.incrementAndGet()
            carregando = true
            lifecycleScope.launch {
                val r = Repo.carregar(ctx)
                if (meu == pedidos.get()) {
                    r.onSuccess { mercado = it; erro = null }
                     .onFailure { erro = getString(R.string.sem_rede) }
                    carregando = false
                    WidgetCotacao.redesenharTodos(ctx)
                }
            }
        }

        /* Antes isto era LaunchedEffect(favoritos): cada moeda marcada na tela de
           edição disparava uma busca completa na internet. Marcar cinco moedas
           eram cinco buscas empilhadas — daí a lentidão, e daí moedas marcadas
           que "não carregavam", porque a API cortava o excesso de pedidos.
           Agora a lista é gravada na hora (é local) e a busca acontece uma vez
           só, quando a pessoa termina e fecha a tela. */
        var listaMudou by remember { mutableStateOf(false) }
        var rastreando by remember { mutableStateOf(false) }
        var compartilhando by remember { mutableStateOf(false) }
        /* O convite para cadastrar aparece depois de um dia de uso, não na
           abertura. Quem acabou de instalar quer ver o preço do Bitcoin; um
           formulário na cara é o caminho mais curto para a desinstalação e para
           a avaliação de uma estrela. */
        var mostrarCadastro by remember { mutableStateOf(Cadastro.deveMostrar(ctx)) }
        // ficha que abre ao tocar numa moeda, e a escolha de onde vem o preço
        var fichaDe by remember { mutableStateOf<Moeda?>(null) }
        var escolhendoFonte by remember { mutableStateOf(false) }
        var comparando by remember { mutableStateOf(false) }
        var naCarteira by remember { mutableStateOf(false) }
        var nosAjustes by remember { mutableStateOf(false) }
        var fonte by remember { mutableStateOf(Guardados.fonte(ctx)) }
        LaunchedEffect(Unit) {
            Cadastro.marcarUso(ctx)
            atualizar()
        }

        if (mostrarCadastro) {
            TelaCadastro(
                aoEntrar = { mostrarCadastro = false },
                aoAdiar = { Cadastro.adiar(ctx); mostrarCadastro = false }
            )
            return
        }

        if (rastreando) {
            TelaRastrear(aoFechar = { rastreando = false })
            return
        }

        if (comparando) {
            TelaComparar(mercado = mercado, aoFechar = { comparando = false })
            return
        }

        if (naCarteira) {
            TelaCarteira(mercado = mercado, aoFechar = { naCarteira = false })
            return
        }

        if (nosAjustes) {
            TelaAjustes(
                aoFechar = { nosAjustes = false },
                // trocar de moeda muda o que a API precisa buscar, não só como desenhar
                aoMudarMoeda = { atualizar() },
                aoMudarWidget = { WidgetCotacao.redesenharTodos(ctx) }
            )
            return
        }

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
                aoAlternar = { id ->
                    favoritos = Guardados.alternarFavorito(ctx, id)
                    listaMudou = true
                },
                aoFechar = {
                    editando = false
                    if (listaMudou) { listaMudou = false; atualizar() }
                }
            )
            return
        }

        Scaffold(
            containerColor = Fundo,
            topBar = {
                BarraTopo(
                    aba = aba,
                    carregando = if (aba == Aba.NOTICIAS) carregandoNoticias else carregando,
                    aoAtualizar = if (aba == Aba.NOTICIAS) buscarNoticias else atualizar,
                    aoTrocarIdioma = { tag -> Idioma.salvar(ctx, tag); recreate() },
                    aoRastrear = { rastreando = true },
                    aoCompartilhar = { compartilhando = true },
                    aoCadastrar = if (Cadastro.jaCadastrado(ctx)) null else ({ mostrarCadastro = true }),
                    aoTrocarFonte = { escolhendoFonte = true },
                    aoAjustes = { nosAjustes = true }
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
                        aoTentarDeNovo = atualizar,
                        aoRastrear = { rastreando = true },
                        aoTocarMoeda = { fichaDe = it },
                        fonte = fonte,
                        aoTrocarFonte = { escolhendoFonte = true },
                        aoComparar = { comparando = true },
                        aoAbrirCarteira = { naCarteira = true }
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
                if (compartilhando) DialogoCompartilhar(mercado) { compartilhando = false }
                fichaDe?.let { m ->
                    FichaMoeda(
                        // segue a atualização: se o preço mudar com a ficha aberta, ela muda junto
                        moeda = mercado.acharPor(m.id) ?: m,
                        aoCriarAlerta = {
                            fichaDe = null
                            abaIndice = Aba.ALERTAS.ordinal
                        },
                        aoFechar = { fichaDe = null }
                    )
                }
                if (escolhendoFonte) DialogoFonte(
                    atual = fonte,
                    aoEscolher = { id ->
                        escolhendoFonte = false
                        if (id != fonte) {
                            fonte = id
                            Guardados.salvarFonte(ctx, id)
                            atualizar()
                        }
                    },
                    aoFechar = { escolhendoFonte = false }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BarraTopo(
        aba: Aba,
        carregando: Boolean,
        aoAtualizar: () -> Unit,
        aoTrocarIdioma: (String) -> Unit,
        aoRastrear: () -> Unit,
        aoCompartilhar: () -> Unit,
        aoCadastrar: (() -> Unit)?,
        aoTrocarFonte: () -> Unit,
        aoAjustes: () -> Unit
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // a marca de verdade, não uma bolinha genérica
                    Image(
                        painter = painterResource(R.drawable.logo_kriptobr),
                        contentDescription = "KriptoBR",
                        modifier = Modifier.height(21.dp)
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(stringResource(aba.rotulo), color = Apagado, fontSize = 13.sp)
                }
            },
            actions = {
                BotaoIdioma(aoTrocarIdioma)
                MenuExtras(aoRastrear, aoCompartilhar, aoCadastrar, aoTrocarFonte, aoAjustes)
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

    /**
     * Escolha do idioma. O padrão é seguir o aparelho, mas muita gente usa o
     * celular em inglês e prefere ler em português — então a palavra final é da
     * pessoa. Trocar recria a tela, que é como o Android relê as frases.
     */
    @Composable
    private fun BotaoIdioma(aoTrocar: (String) -> Unit) {
        val ctx = LocalContext.current
        var aberto by remember { mutableStateOf(false) }
        val escolha = remember { Idioma.escolha(ctx) }
        val opcoes = listOf(
            Idioma.AUTO to stringResource(R.string.idioma_auto),
            "pt" to "Português",
            "en" to "English",
            "es" to "Español"
        )
        Box {
            TextButton(onClick = { aberto = true }) {
                Text(
                    Idioma.atual(ctx).uppercase(),
                    color = Tinta2, fontWeight = FontWeight.Bold, fontSize = 12.5f.sp
                )
            }
            DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
                opcoes.forEach { (tag, rotulo) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                rotulo,
                                color = if (tag == escolha) Mint else Tinta,
                                fontWeight = if (tag == escolha) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        onClick = { aberto = false; if (tag != escolha) aoTrocar(tag) }
                    )
                }
            }
        }
    }

    /**
     * Menu de três pontos. Rastrear e compartilhar não ganharam aba própria de
     * propósito: seis abas na barra de baixo ficam ilegíveis num celular
     * pequeno, e as duas são coisas que se usa de vez em quando, não o tempo todo.
     */
    @Composable
    private fun MenuExtras(
        aoRastrear: () -> Unit,
        aoCompartilhar: () -> Unit,
        aoCadastrar: (() -> Unit)?,
        aoTrocarFonte: () -> Unit,
        aoAjustes: () -> Unit
    ) {
        var aberto by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { aberto = true }) {
                Icon(Icons.Filled.MoreVert, stringResource(R.string.mais_opcoes), tint = Tinta2)
            }
            DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.rastrear_titulo), color = Tinta, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Mint) },
                    onClick = { aberto = false; aoRastrear() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.compartilhar_titulo), color = Tinta, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Share, null, tint = Mint) },
                    onClick = { aberto = false; aoCompartilhar() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.fonte_titulo), color = Tinta, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Refresh, null, tint = Mint) },
                    onClick = { aberto = false; aoTrocarFonte() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.ajustes_titulo), color = Tinta, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Settings, null, tint = Mint) },
                    onClick = { aberto = false; aoAjustes() }
                )
                if (aoCadastrar != null) DropdownMenuItem(
                    text = { Text(stringResource(R.string.receber_cupom), color = Tinta, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Star, null, tint = Mint) },
                    onClick = { aberto = false; aoCadastrar() }
                )
            }
        }
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
