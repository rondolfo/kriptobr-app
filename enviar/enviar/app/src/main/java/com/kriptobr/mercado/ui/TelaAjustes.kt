package com.kriptobr.mercado.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptobr.mercado.R
import com.kriptobr.mercado.dados.Ajustes

/**
 * Ajustes: moeda, widget e avisos.
 *
 * A moeda saiu de dentro do idioma. Eram duas escolhas coladas numa só — quem
 * lia o app em inglês não conseguia ver preço em real, e tem brasileiro que
 * acompanha o mercado em dólar. Agora são independentes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaAjustes(aoFechar: () -> Unit, aoMudarMoeda: () -> Unit, aoMudarWidget: () -> Unit) {
    val ctx = LocalContext.current
    var moeda by remember { mutableStateOf(Ajustes.moeda(ctx)) }
    var quantas by remember { mutableIntStateOf(Ajustes.widgetQuantas(ctx)) }
    var faixa by remember { mutableStateOf(Ajustes.widgetFaixa(ctx)) }
    var volume by remember { mutableStateOf(Ajustes.widgetVolume(ctx)) }
    var avisoNoticias by remember { mutableStateOf(Ajustes.avisoNoticias(ctx)) }
    var avisoResumo by remember { mutableStateOf(Ajustes.avisoResumo(ctx)) }

    Scaffold(
        containerColor = Fundo,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ajustes_titulo), color = Tinta, fontSize = 17.sp) },
                navigationIcon = {
                    Text("✕", color = Tinta2, fontSize = 17.sp,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clicavel(aoFechar).padding(14.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Fundo)
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp)
        ) {
            // ------------------------------------------------------- moeda
            Secao(stringResource(R.string.ajustes_moeda))
            Cartao(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Superficie2)) {
                    listOf(
                        Ajustes.AUTO to stringResource(R.string.moeda_auto),
                        "brl" to "R$ BRL",
                        "usd" to "US$ USD"
                    ).forEach { (id, rotulo) ->
                        Pedaco(rotulo, moeda == id, Modifier.weight(1f)) {
                            moeda = id
                            Ajustes.salvarMoeda(ctx, id)
                            aoMudarMoeda()
                        }
                    }
                }
                Text(
                    stringResource(R.string.ajustes_moeda_ajuda), color = Apagado,
                    fontSize = 11.5f.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 9.dp)
                )
            }

            // ------------------------------------------------------ widget
            Secao(stringResource(R.string.ajustes_widget))
            Cartao(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.widget_quantas), color = Tinta2, fontSize = 12.5f.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Superficie2)) {
                    (0..5).forEach { n ->
                        Pedaco(n.toString(), quantas == n, Modifier.weight(1f)) {
                            quantas = n
                            Ajustes.salvarWidgetQuantas(ctx, n)
                            aoMudarWidget()
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Chave(stringResource(R.string.widget_mostrar_faixa), faixa) {
                    faixa = it; Ajustes.salvarWidgetFaixa(ctx, it); aoMudarWidget()
                }
                Chave(stringResource(R.string.widget_mostrar_volume), volume) {
                    volume = it; Ajustes.salvarWidgetVolume(ctx, it); aoMudarWidget()
                }
                Text(
                    stringResource(R.string.widget_ajuda), color = Apagado,
                    fontSize = 11.5f.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 6.dp)
                )
            }

            // ------------------------------------------------------ avisos
            Secao(stringResource(R.string.ajustes_avisos))
            Cartao(Modifier.fillMaxWidth()) {
                Chave(stringResource(R.string.aviso_resumo_rotulo), avisoResumo) {
                    avisoResumo = it; Ajustes.salvarAvisoResumo(ctx, it)
                }
                Text(
                    stringResource(R.string.aviso_resumo_ajuda), color = Apagado,
                    fontSize = 11.5f.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 10.dp)
                )
                Chave(stringResource(R.string.aviso_noticias_rotulo), avisoNoticias) {
                    avisoNoticias = it; Ajustes.salvarAvisoNoticias(ctx, it)
                }
                Text(
                    stringResource(R.string.aviso_noticias_ajuda), color = Apagado,
                    fontSize = 11.5f.sp, lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.ajustes_rodape), color = Apagado,
                fontSize = 11.sp, lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun Secao(texto: String) {
    Text(
        texto, color = Apagado, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun Pedaco(texto: String, ligado: Boolean, modifier: Modifier, aoTocar: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (ligado) Mint else Color.Transparent)
            .clicavel(aoTocar)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto, color = if (ligado) MintTinta else Tinta2,
            fontWeight = FontWeight.SemiBold, fontSize = 12.5f.sp, maxLines = 1
        )
    }
}

@Composable
private fun Chave(rotulo: String, ligado: Boolean, aoMudar: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(rotulo, color = Tinta, fontSize = 13.5f.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = ligado, onCheckedChange = aoMudar,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = Mint,
                uncheckedThumbColor = Tinta2, uncheckedTrackColor = Superficie2,
                uncheckedBorderColor = Borda
            )
        )
    }
}
