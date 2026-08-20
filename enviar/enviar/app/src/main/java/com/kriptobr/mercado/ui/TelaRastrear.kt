package com.kriptobr.mercado.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptobr.mercado.R
import com.kriptobr.mercado.dados.Destino
import com.kriptobr.mercado.dados.Enderecos

/**
 * Rastreador: cola o endereço da carteira ou o código da transação e o app abre
 * o explorador de blocos da rede certa.
 *
 * O reconhecimento é local — o formato do endereço já diz de que rede ele é.
 * A única ambiguidade real é o endereço que começa com 0x, que é idêntico na
 * Ethereum, BNB Chain, Polygon e nas outras; nesse caso o app mostra a lista em
 * vez de escolher sozinho, porque mandar a pessoa para a rede errada é pior do
 * que pedir um toque a mais.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaRastrear(aoFechar: () -> Unit) {
    val ctx = LocalContext.current
    var texto by remember { mutableStateOf("") }
    var buscou by remember { mutableStateOf(false) }
    val destinos = remember(texto, buscou) {
        if (!buscou) emptyList() else Enderecos.identificar(texto)
    }

    Scaffold(
        containerColor = Fundo,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rastrear_titulo), color = Tinta, fontSize = 17.sp) },
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
                .padding(horizontal = 16.dp)
        ) {
            Text(stringResource(R.string.rastrear_ajuda), color = Apagado,
                fontSize = 12.5f.sp, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 14.dp))

            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it; buscou = false },
                label = { Text(stringResource(R.string.endereco_ou_hash), color = Apagado, fontSize = 13.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                minLines = 2,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Mint, unfocusedBorderColor = Borda,
                    focusedTextColor = Tinta, unfocusedTextColor = Tinta,
                    cursorColor = Mint,
                    focusedContainerColor = Superficie, unfocusedContainerColor = Superficie
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { colarDaAreaDeTransferencia(ctx)?.let { texto = it; buscou = true } },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text(stringResource(R.string.colar), color = Tinta2, fontSize = 14.sp) }
                Button(
                    onClick = { buscou = true },
                    enabled = texto.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = MintTinta,
                        disabledContainerColor = Superficie2, disabledContentColor = Apagado),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text(stringResource(R.string.rastrear), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            }

            Spacer(Modifier.height(20.dp))

            if (buscou && destinos.isEmpty()) {
                Cartao(Modifier.fillMaxWidth(), corBorda = Baixa.copy(alpha = 0.5f)) {
                    Text(stringResource(R.string.nao_reconhecido), color = Tinta,
                        fontSize = 13.sp, lineHeight = 18.sp)
                }
            }

            if (destinos.isNotEmpty()) {
                val transacao = destinos.first().tipo == Destino.Tipo.TRANSACAO
                Text(
                    stringResource(if (transacao) R.string.parece_transacao else R.string.parece_carteira),
                    color = Apagado, fontSize = 11.5f.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (destinos.size > 1) {
                    Text(stringResource(R.string.escolha_a_rede), color = Tinta2,
                        fontSize = 12.5f.sp, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 10.dp))
                }
                destinos.forEach { d ->
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Superficie)
                            .clicavel { abrir(ctx, d.url) }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Selo(d.moeda, 30.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.rede, color = Tinta, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(explorador(d.url), color = Apagado, fontSize = 11.sp, maxLines = 1)
                        }
                        Text("→", color = Mint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

private fun explorador(url: String): String =
    url.removePrefix("https://").substringBefore('/')

private fun abrir(ctx: Context, url: String) {
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun colarDaAreaDeTransferencia(ctx: Context): String? = runCatching {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()?.trim()
}.getOrNull()?.takeIf { it.isNotBlank() }
