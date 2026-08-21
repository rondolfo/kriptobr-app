package com.kriptobr.mercado.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptobr.mercado.R
import com.kriptobr.mercado.dados.Corretoras
import com.kriptobr.mercado.dados.Formato
import com.kriptobr.mercado.dados.Moeda

/**
 * A ficha que abre ao tocar numa moeda.
 *
 * Antes tocar não fazia nada — a lista era um beco sem saída. É aqui que entram
 * a máxima e a mínima do dia e o volume negociado: números que não cabem na
 * linha da lista sem espremer tudo, mas que quem acompanha o mercado procura.
 *
 * A barra da faixa do dia é o coração da tela. Ver que o preço está colado na
 * máxima, ou caindo em direção à mínima, diz mais em um segundo do que três
 * números soltos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FichaMoeda(
    moeda: Moeda,
    aoCriarAlerta: (Moeda) -> Unit,
    aoFechar: () -> Unit
) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = estado,
        containerColor = Superficie,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Borda) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Selo(moeda.simbolo, 38.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(moeda.nome, color = Tinta, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(moeda.simbolo, color = Apagado, fontSize = 12.5f.sp)
                }
                Variacao(moeda.variacao24h, grande = true)
            }

            Spacer(Modifier.height(14.dp))
            Text(
                Formato.dinheiroCheio(moeda.preco),
                color = Tinta, fontWeight = FontWeight.Bold, fontSize = 30.sp
            )
            EtiquetaFonte(moeda)

            if (moeda.historico.size > 3) {
                Spacer(Modifier.height(16.dp))
                MiniGrafico(
                    moeda.historico,
                    if (moeda.variacao24h >= 0) Alta else Baixa,
                    Modifier.fillMaxWidth().height(74.dp)
                )
                Text(
                    stringResource(R.string.ficha_7dias),
                    color = Apagado, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (moeda.minima24h > 0.0 && moeda.maxima24h > moeda.minima24h) {
                Spacer(Modifier.height(20.dp))
                FaixaDoDia(moeda)
            }

            Spacer(Modifier.height(18.dp))
            if (moeda.volume24h > 0.0) {
                LinhaDado(stringResource(R.string.ficha_volume), Formato.compacto(moeda.volume24h))
            }
            if (moeda.capMercado > 0.0) {
                LinhaDado(stringResource(R.string.ficha_cap), Formato.compacto(moeda.capMercado))
            }
            if (moeda.maxima24h > 0.0) {
                LinhaDado(stringResource(R.string.ficha_maxima), Formato.dinheiroCheio(moeda.maxima24h))
            }
            if (moeda.minima24h > 0.0) {
                LinhaDado(stringResource(R.string.ficha_minima), Formato.dinheiroCheio(moeda.minima24h))
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { aoCriarAlerta(moeda) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = MintTinta)
            ) {
                Text(stringResource(R.string.ficha_criar_alerta), fontWeight = FontWeight.Bold, fontSize = 14.5f.sp)
            }
        }
    }
}

/** Diz de onde veio o número, e avisa quando ele passou por conversão de moeda. */
@Composable
private fun EtiquetaFonte(moeda: Moeda) {
    val fonte = moeda.fonte
    if (fonte.isEmpty() || fonte == Corretoras.MEDIA) {
        Text(stringResource(R.string.fonte_media), color = Apagado, fontSize = 11.5f.sp)
        return
    }
    val nome = Corretoras.nomeDe(fonte)
    Text(
        if (moeda.convertido) stringResource(R.string.fonte_convertida, nome)
        else stringResource(R.string.fonte_direta, nome),
        color = Apagado, fontSize = 11.5f.sp
    )
}

/**
 * Barra que mostra onde o preço de agora está entre a mínima e a máxima do dia.
 * A bolinha é a posição atual; as pontas trazem os dois extremos escritos.
 */
@Composable
private fun FaixaDoDia(moeda: Moeda) {
    val faixa = moeda.maxima24h - moeda.minima24h
    val posicao = ((moeda.preco - moeda.minima24h) / faixa).coerceIn(0.0, 1.0).toFloat()
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ficha_faixa), color = Apagado, fontSize = 11.5f.sp)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Superficie2)
        ) {
            /* A fatia colorida vai da mínima até o preço de agora — mais legível
               do que só um ponto solto sobre uma barra cinza. */
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(posicao.coerceAtLeast(0.02f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (moeda.variacao24h >= 0) Alta else Baixa)
            )
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(Formato.dinheiro(moeda.minima24h), color = Baixa, fontSize = 12.sp)
            Text(Formato.dinheiro(moeda.maxima24h), color = Alta, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LinhaDado(rotulo: String, valor: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(rotulo, color = Apagado, fontSize = 13.sp)
        Text(valor, color = Tinta, fontWeight = FontWeight.SemiBold, fontSize = 13.5f.sp)
    }
    HorizontalDivider(color = Borda.copy(alpha = 0.5f))
}
