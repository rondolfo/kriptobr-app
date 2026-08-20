package com.kriptobr.mercado.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptobr.mercado.dados.Formato

/** Atalho para deixar as chamadas de clique curtas e legíveis. */
fun Modifier.clicavel(aoTocar: () -> Unit): Modifier = this.clickable(onClick = aoTocar)

/** Bolinha com a inicial da moeda — nunca depende de baixar logo nenhum. */
@Composable
fun Selo(simbolo: String, tamanho: Dp = 26.dp) {
    Box(
        Modifier.size(tamanho).clip(CircleShape).background(corDaMoeda(simbolo)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            simbolo.take(1).uppercase(),
            color = Color(0xFF0B0B0B),
            fontWeight = FontWeight.ExtraBold,
            fontSize = (tamanho.value * 0.46f).sp
        )
    }
}

/** Etiqueta de variação, verde para alta e vermelha para queda. */
@Composable
fun Variacao(v: Double, grande: Boolean = false) {
    val cor = if (v >= 0) Alta else Baixa
    Box(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(cor.copy(alpha = 0.15f))
            .padding(horizontal = if (grande) 10.dp else 7.dp, vertical = if (grande) 4.dp else 2.dp)
    ) {
        Text(
            Formato.porcento(v),
            color = cor,
            fontWeight = FontWeight.Bold,
            fontSize = if (grande) 13.sp else 11.5f.sp
        )
    }
}

@Composable
fun Cartao(
    modifier: Modifier = Modifier,
    corBorda: Color = Borda,
    fundo: Color = Superficie,
    conteudo: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(fundo)
            .border(1.dp, corBorda, RoundedCornerShape(16.dp))
            .padding(14.dp),
        content = conteudo
    )
}

@Composable
fun TituloSecao(texto: String, acao: String? = null, aoTocarAcao: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto.uppercase(), color = Apagado, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
        if (acao != null) {
            Spacer(Modifier.weight(1f))
            Text(
                acao, color = Mint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .then(if (aoTocarAcao != null) Modifier.clicavel(aoTocarAcao) else Modifier)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

/** Mini-gráfico dos últimos dias. Sem eixos, sem números — só a forma do movimento. */
@Composable
fun MiniGrafico(pontos: List<Double>, cor: Color, modifier: Modifier = Modifier) {
    if (pontos.size < 2) {
        Spacer(modifier)
        return
    }
    Canvas(modifier) { desenharLinha(pontos, cor) }
}

private fun DrawScope.desenharLinha(pontos: List<Double>, cor: Color) {
    val min = pontos.min()
    val max = pontos.max()
    val amplitude = (max - min).takeIf { it > 0 } ?: 1.0
    val passo = size.width / (pontos.size - 1)
    val y = { v: Double -> (size.height - 3f - ((v - min) / amplitude * (size.height - 6f))).toFloat() }

    val linha = Path().apply {
        moveTo(0f, y(pontos[0]))
        pontos.forEachIndexed { i, v -> if (i > 0) lineTo(passo * i, y(v)) }
    }
    val area = Path().apply {
        addPath(linha)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(area, Brush.verticalGradient(listOf(cor.copy(alpha = 0.22f), cor.copy(alpha = 0f))))
    drawPath(linha, cor, style = Stroke(width = 2.2f.dp.toPx()))
}
