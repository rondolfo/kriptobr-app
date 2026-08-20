package com.kriptobr.mercado.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.kriptobr.mercado.R
import com.kriptobr.mercado.dados.CartaoImagem
import com.kriptobr.mercado.dados.Mercado
import kotlinx.coroutines.launch

/**
 * Escolha do formato da imagem antes de compartilhar.
 *
 * Cada lugar tem o seu recorte: o Stories corta o que não for 9:16, o feed do
 * Instagram é quadrado e no WhatsApp e no X a imagem deitada rende mais. Gerar
 * no formato certo evita a marca aparecer cortada.
 */
@Composable
fun DialogoCompartilhar(mercado: Mercado, aoFechar: () -> Unit) {
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var gerando by remember { mutableStateOf<CartaoImagem.Tamanho?>(null) }

    val escolher: (CartaoImagem.Tamanho) -> Unit = { t ->
        gerando = t
        escopo.launch {
            val arquivo = CartaoImagem.gerar(ctx, mercado, t)
            gerando = null
            if (arquivo != null) {
                runCatching {
                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.arquivos", arquivo)
                    val i = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, ctx.getString(R.string.compartilhar_legenda))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(Intent.createChooser(i, ctx.getString(R.string.compartilhar))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            aoFechar()
        }
    }

    AlertDialog(
        onDismissRequest = { if (gerando == null) aoFechar() },
        containerColor = Superficie,
        title = {
            Text(stringResource(R.string.compartilhar_titulo), color = Tinta,
                fontSize = 17.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(stringResource(R.string.compartilhar_texto), color = Apagado,
                    fontSize = 12.5f.sp, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 14.dp))
                Opcao(R.string.formato_story, R.string.formato_story_desc, 9f / 16f,
                    gerando == CartaoImagem.Tamanho.STORY) { escolher(CartaoImagem.Tamanho.STORY) }
                Opcao(R.string.formato_post, R.string.formato_post_desc, 1f,
                    gerando == CartaoImagem.Tamanho.POST) { escolher(CartaoImagem.Tamanho.POST) }
                Opcao(R.string.formato_deitado, R.string.formato_deitado_desc, 16f / 9f,
                    gerando == CartaoImagem.Tamanho.DEITADO) { escolher(CartaoImagem.Tamanho.DEITADO) }
            }
        },
        confirmButton = {
            TextButton(onClick = aoFechar, enabled = gerando == null) {
                Text(stringResource(R.string.fechar), color = Tinta2)
            }
        }
    )
}

@Composable
private fun Opcao(
    titulo: Int,
    descricao: Int,
    proporcao: Float,
    ocupado: Boolean,
    aoTocar: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Superficie2)
            .clicavel { if (!ocupado) aoTocar() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // miniatura na proporção real do formato, para a escolha ser visual
        val altura = 34.dp
        Box(
            Modifier
                .height(altura)
                .width(altura * proporcao.coerceAtMost(1.8f))
                .clip(RoundedCornerShape(4.dp))
                .background(Mint.copy(alpha = 0.28f))
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(titulo), color = Tinta, fontSize = 13.5f.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(descricao), color = Apagado, fontSize = 11.sp)
        }
        if (ocupado) CircularProgressIndicator(color = Mint, strokeWidth = 2.dp,
            modifier = Modifier.height(18.dp).width(18.dp))
    }
}
