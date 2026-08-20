package com.kriptobr.mercado.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptobr.mercado.R

private data class Item(val titulo: Int, val texto: Int, val emoji: String, val url: String)

private val ITENS = listOf(
    Item(R.string.loja_hw, R.string.loja_hw_txt, "🔐", "https://kriptobr.com/cold-wallet/"),
    Item(R.string.loja_aco, R.string.loja_aco_txt, "🪙", "https://kriptobr.com/acessorios-2/"),
    Item(R.string.loja_curso, R.string.loja_curso_txt, "🎓", "https://kriptobr.com/cursos/"),
    Item(R.string.loja_news, R.string.loja_news_txt, "📰", "https://kriptohoje.com/")
)

@Composable
fun TelaLoja() {
    val ctx = LocalContext.current
    val abrir = { url: String ->
        val alvo = url + (if (url.contains("?")) "&" else "?") + "utm_source=app&utm_medium=aba-loja"
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(alvo))) }
        Unit
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().padding(top = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Mint.copy(alpha = 0.20f), Mint.copy(alpha = 0.03f))))
                .padding(18.dp)
        ) {
            Column {
                Text(stringResource(R.string.loja_chamada), color = Tinta,
                    fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 25.sp)
                Text(stringResource(R.string.loja_sub), color = Tinta2,
                    fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { abrir("https://kriptobr.com/cold-wallet/") },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = MintTinta)
                ) { Text(stringResource(R.string.loja_botao), fontWeight = FontWeight.Bold) }
            }
        }

        TituloSecao(stringResource(R.string.loja_secao))
        ITENS.forEach { item ->
            Cartao(Modifier.fillMaxWidth().padding(bottom = 10.dp).clicavel { abrir(item.url) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Superficie2),
                        contentAlignment = Alignment.Center
                    ) { Text(item.emoji, fontSize = 18.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(item.titulo), color = Tinta,
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(item.texto), color = Tinta2, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                    Text("›", color = Apagado, fontSize = 20.sp)
                }
            }
        }

        Cartao(Modifier.fillMaxWidth().padding(top = 4.dp), corBorda = Btc.copy(alpha = 0.5f)) {
            Text(stringResource(R.string.loja_aviso_titulo), color = Btc,
                fontSize = 12.5f.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.loja_aviso), color = Tinta2,
                fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}
