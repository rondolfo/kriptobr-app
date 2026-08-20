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
    aoTocarMoeda: (Moeda) -> Unit
) {
    val btc = mercado.acharPor("bitcoin")
    val secundarias = listOf("ethereum", "solana").mapNotNull { mercado.acharPor(it) }
    val lista = mercado.moedas.filter { it.id != "bitcoin" && it.id !in secundarias.map { s -> s.id } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        item {
            if (erro != null) FaixaAviso(erro, mercado.atualizadoEm)
            if (btc != null) DestaqueBitcoin(btc) { aoTocarMoeda(btc) }
            else if (carregando) EsqueletoDestaque()
        }
        if (secundarias.isNotEmpty()) item {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                secundarias.forEach { m ->
                    Box(Modifier.weight(1f)) { CartaoPequeno(m) { aoTocarMoeda(m) } }
                }
            }
        }
        mercado.medo?.let { fg ->
            item { TituloSecao(stringResource(R.string.termometro)) }
            item { CartaoMedo(fg) }
        }
        item { TituloSecao(stringResource(R.string.minha_lista), stringResource(R.string.editar), aoEditarLista) }
        items(lista, key = { it.id }) { m -> LinhaMoeda(m) { aoTocarMoeda(m) } }
        if (lista.isEmpty() && !carregando) item { VaziaLista(aoEditarLista) }
    }
}

@Composable
private fun FaixaAviso(texto: String, quando: Long) {
    Cartao(Modifier.fillMaxWidth().padding(top = 4.dp), corBorda = Btc.copy(alpha = 0.5f), fundo = Superficie2) {
        Text(texto, color = Tinta2, fontSize = 12.5f.sp, lineHeight = 17.sp)
        if (quando > 0) Text(
            stringResource(R.string.mostrando_de, Formato.hora(quando)),
            color = Apagado, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)
        )
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
