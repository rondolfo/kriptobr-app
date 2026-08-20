package com.kriptobr.mercado.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptobr.mercado.R
import com.kriptobr.mercado.dados.Miniaturas
import com.kriptobr.mercado.dados.Noticia
import com.kriptobr.mercado.dados.Noticias

/**
 * Aba de notícias: lista de manchetes com miniatura pequena.
 *
 * A escolha aqui é density — cabem quatro vezes mais notícias do que caberiam
 * em cartões com foto grande, e é isso que a pessoa quer quando abre para ver
 * "o que aconteceu". O ponto verde marca o que saiu depois da última visita.
 */
@Composable
fun TelaNoticias(
    noticias: List<Noticia>,
    carregando: Boolean,
    erro: String?,
    corteNovas: Long,
    aoAbrir: (Noticia) -> Unit
) {
    if (noticias.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                carregando -> CircularProgressIndicator(color = Mint, strokeWidth = 2.dp)
                else -> Text(
                    erro ?: stringResource(R.string.noticias_vazio),
                    color = Apagado, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 34.dp)
                )
            }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp)) {
                Text(
                    stringResource(R.string.noticias_fonte),
                    color = Tinta, fontWeight = FontWeight.Bold, fontSize = 17.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(R.string.noticias_ajuda),
                    color = Apagado, fontSize = 12.sp, lineHeight = 16.sp
                )
            }
        }
        items(noticias, key = { it.link }) { n ->
            LinhaNoticia(n, nova = corteNovas > 0L && n.quando > corteNovas) { aoAbrir(n) }
            HorizontalDivider(color = Borda.copy(alpha = 0.55f), thickness = 0.6.dp)
        }
        item {
            Text(
                stringResource(R.string.noticias_rodape),
                color = Apagado, fontSize = 11.5f.sp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }
}

@Composable
private fun LinhaNoticia(n: Noticia, nova: Boolean, aoTocar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clicavel(aoTocar)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.Top
    ) {
        Miniatura(n.miniatura)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                n.titulo,
                color = Tinta,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 18.5f.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (nova) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Mint))
                    Spacer(Modifier.width(6.dp))
                }
                Text(quandoEmPalavras(n.quando), color = Apagado, fontSize = 11.5f.sp)
            }
        }
    }
}

/** 56dp de lado; enquanto a foto não chega, o bloco da marca segura o lugar. */
@Composable
private fun Miniatura(url: String?) {
    val ctx = LocalContext.current
    val densidade = androidx.compose.ui.platform.LocalDensity.current
    val ladoPx = remember(densidade) { with(densidade) { 56.dp.roundToPx() } }
    var foto by remember(url) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(url) {
        if (url.isNullOrBlank()) return@LaunchedEffect
        foto = runCatching { Miniaturas.carregar(ctx, url, ladoPx) }.getOrNull()
    }

    Box(
        Modifier.size(56.dp).clip(RoundedCornerShape(9.dp)).background(Superficie2),
        contentAlignment = Alignment.Center
    ) {
        val b = foto
        if (b != null) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text("₿", color = Mint.copy(alpha = 0.55f), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
    }
}

/** "há 20 minutos", "ontem" — o próprio Android já sabe dizer isso no idioma do aparelho. */
private fun quandoEmPalavras(quando: Long): String {
    if (quando <= 0L) return ""
    return DateUtils.getRelativeTimeSpanString(
        quando, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}

/** Abre a notícia no navegador do aparelho, marcando a origem para o portal. */
fun abrirNoticia(ctx: android.content.Context, n: Noticia) {
    runCatching {
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(Noticias.comUtm(n.link)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
