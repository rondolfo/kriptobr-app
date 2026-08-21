package com.kriptobr.mercado.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptobr.mercado.R
import com.kriptobr.mercado.dados.Corretoras
import com.kriptobr.mercado.dados.Formato
import com.kriptobr.mercado.dados.MedoGanancia
import com.kriptobr.mercado.dados.Mercado
import com.kriptobr.mercado.dados.Moeda

@Composable
fun TelaMercado(
    mercado: Mercado,
    carregando: Boolean,
    erro: String?,
    aoEditarLista: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    aoRastrear: () -> Unit,
    aoTocarMoeda: (Moeda) -> Unit,
    fonte: String,
    aoTrocarFonte: () -> Unit
) {
    val btc = mercado.acharPor("bitcoin")
    val secundarias = listOf("ethereum", "solana").mapNotNull { mercado.acharPor(it) }
    val lista = mercado.moedas.filter { it.id != "bitcoin" && it.id !in secundarias.map { s -> s.id } }
    val temDados = mercado.moedas.isNotEmpty()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        /* Três situações diferentes, três avisos diferentes. O erro só vira
           alarme quando não há nada para mostrar; com preços na tela ele é
           apenas uma nota de rodapé dizendo de quando eles são. */
        if (erro != null && !carregando) item {
            if (temDados) NotaDesatualizado(mercado.atualizadoEm)
            else CartaoFalhou(aoTentarDeNovo)
        }
        item {
            if (btc != null) DestaqueBitcoin(btc) { aoTocarMoeda(btc) }
            else if (carregando || erro == null) EsqueletoDestaque()
        }
        if (secundarias.isNotEmpty()) item {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                secundarias.forEach { m ->
                    Box(Modifier.weight(1f)) { CartaoPequeno(m) { aoTocarMoeda(m) } }
                }
            }
        }
        /* De onde vêm os preços fica à vista, e não escondido no menu: quem
           trocou para a Binance precisa lembrar disso ao olhar o número. */
        if (temDados) item { LinhaFonte(fonte, aoTrocarFonte) }
        mercado.medo?.let { fg ->
            item { TituloSecao(stringResource(R.string.termometro)) }
            item { CartaoMedo(fg) }
        }
        /* O rastreador estava escondido no menu de três pontos e ninguém achava.
           Aqui ele fica no caminho de quem rola a tela — que é todo mundo. */
        if (temDados) item { AtalhoRastrear(aoRastrear) }
        if (temDados || !carregando) item {
            TituloSecao(stringResource(R.string.minha_lista), stringResource(R.string.editar), aoEditarLista)
        }
        items(lista, key = { it.id }) { m -> LinhaMoeda(m) { aoTocarMoeda(m) } }
        // "sua lista está vazia" só quando ela está mesmo vazia — não quando a
        // internet falhou, que era o caso em que mais aparecia
        if (lista.isEmpty() && !carregando && erro == null) item { VaziaLista(aoEditarLista) }
    }
}

/** Linha discreta dizendo de qual corretora vêm os números — e como trocar. */
@Composable
private fun LinhaFonte(fonte: String, aoTrocar: () -> Unit) {
    val nome = if (fonte.isEmpty() || fonte == Corretoras.MEDIA)
        stringResource(R.string.fonte_media_nome) else Corretoras.nomeDe(fonte)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .clicavel(aoTrocar)
            .background(Superficie)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.fonte_titulo), color = Apagado, fontSize = 11.5f.sp)
        Spacer(Modifier.width(8.dp))
        Text(nome, color = Tinta, fontWeight = FontWeight.SemiBold, fontSize = 12.5f.sp)
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.trocar), color = Mint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

/** Tem preço na tela, só não é o mais fresco: uma linha discreta, sem moldura de alerta. */
@Composable
private fun AtalhoRastrear(aoTocar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Mint.copy(alpha = 0.10f))
            .clicavel(aoTocar)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Mint.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) { Text("\uD83D\uDD0E", fontSize = 17.sp) }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.rastrear_titulo), color = Tinta,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.rastrear_chamada), color = Tinta2,
                fontSize = 11.5f.sp, lineHeight = 16.sp)
        }
        Text("\u2192", color = Mint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NotaDesatualizado(quando: Long) {
    Text(
        if (quando > 0) stringResource(R.string.precos_de, Formato.hora(quando))
        else stringResource(R.string.sem_rede),
        color = Apagado, fontSize = 11.5f.sp, lineHeight = 16.sp,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp)
    )
}

/** Não veio nada: aí sim vale o alerta — e principalmente um botão para tentar de novo. */
@Composable
private fun CartaoFalhou(aoTentar: () -> Unit) {
    Cartao(Modifier.fillMaxWidth().padding(top = 6.dp), corBorda = Btc.copy(alpha = 0.5f), fundo = Superficie2) {
        Text(stringResource(R.string.nao_carregou), color = Tinta, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(10.dp))
        Button(onClick = aoTentar, colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = MintTinta)) {
            Text(stringResource(R.string.tentar_de_novo), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DestaqueBitcoin(m: Moeda, aoTocar: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Btc.copy(alpha = 0.17f), Btc.copy(alpha = 0.02f))))
            .clicavel(aoTocar)
    ) {
        MiniGrafico(
            m.historico, Btc,
            Modifier.align(Alignment.BottomEnd).fillMaxWidth(0.52f).height(62.dp)
        )
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Selo(m.simbolo, 24.dp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(m.nome, color = Tinta, fontWeight = FontWeight.SemiBold, fontSize = 13.5f.sp)
                    Text(m.simbolo, color = Apagado, fontSize = 11.sp)
                }
            }
            Text(
                Formato.dinheiro(m.preco),
                color = Tinta, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Variacao(m.variacao24h, grande = true)
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.em_24h), color = Apagado, fontSize = 11.5f.sp)
            }
        }
    }
}

@Composable
private fun CartaoPequeno(m: Moeda, aoTocar: () -> Unit) {
    Cartao(Modifier.fillMaxWidth().clicavel(aoTocar)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Selo(m.simbolo, 17.dp)
            Spacer(Modifier.width(6.dp))
            Text(m.nome.uppercase(), color = Apagado, fontSize = 10.5f.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, maxLines = 1)
        }
        Text(Formato.dinheiro(m.preco), color = Tinta, fontSize = 17.sp,
            fontWeight = FontWeight.Bold, maxLines = 1,
            modifier = Modifier.padding(top = 4.dp))
        Text(Formato.porcento(m.variacao24h), fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = if (m.variacao24h >= 0) Alta else Baixa)
    }
}

@Composable
private fun CartaoMedo(fg: MedoGanancia) {
    val cor = when {
        fg.valor < 25 -> Baixa
        fg.valor < 45 -> Color(0xFFE8894B)
        fg.valor < 55 -> Color(0xFFE8B84B)
        fg.valor < 75 -> Alta
        else -> Alta
    }
    Cartao(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${fg.valor}", color = cor, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(rotuloMedo(fg.valor), color = Tinta, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                val partes = buildList {
                    fg.ontem?.let { add(stringResource(R.string.ontem_n, it)) }
                    fg.semana?.let { add(stringResource(R.string.ha7_n, it)) }
                }
                if (partes.isNotEmpty()) Text(partes.joinToString(" · "), color = Apagado, fontSize = 11.sp)
                Box(
                    Modifier.fillMaxWidth().padding(top = 7.dp).height(6.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Brush.horizontalGradient(listOf(Baixa, Color(0xFFE8B84B), Alta)))
                )
            }
        }
    }
}

@Composable
private fun rotuloMedo(v: Int): String = stringResource(
    when {
        v < 25 -> R.string.medo_extremo
        v < 45 -> R.string.medo
        v < 55 -> R.string.neutro
        v < 75 -> R.string.ganancia
        else -> R.string.ganancia_extrema
    }
)

@Composable
private fun LinhaMoeda(m: Moeda, aoTocar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clicavel(aoTocar).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Selo(m.simbolo, 26.dp)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(m.nome, color = Tinta, fontSize = 13.5f.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(m.simbolo, color = Apagado, fontSize = 11.sp)
        }
        if (m.historico.size > 3) {
            MiniGrafico(
                m.historico, if (m.variacao24h >= 0) Alta else Baixa,
                Modifier.width(52.dp).height(26.dp).padding(end = 10.dp)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Formato.dinheiro(m.preco), color = Tinta, fontSize = 13.5f.sp, fontWeight = FontWeight.SemiBold)
            Text(Formato.porcento(m.variacao24h), fontSize = 11.5f.sp, fontWeight = FontWeight.Bold,
                color = if (m.variacao24h >= 0) Alta else Baixa)
        }
    }
    HorizontalDivider(color = Borda.copy(alpha = 0.55f))
}

@Composable
private fun VaziaLista(aoEditar: () -> Unit) {
    Cartao(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(stringResource(R.string.lista_vazia), color = Tinta2, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(10.dp))
        Button(onClick = aoEditar, colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = MintTinta)) {
            Text(stringResource(R.string.escolher_moedas), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EsqueletoDestaque() {
    Box(
        Modifier.fillMaxWidth().padding(top = 6.dp).height(150.dp)
            .clip(RoundedCornerShape(20.dp)).background(Superficie)
    )
}
