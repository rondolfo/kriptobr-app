package com.kriptobr.mercado.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptobr.mercado.R
import com.kriptobr.mercado.dados.Api
import com.kriptobr.mercado.dados.Corretoras
import com.kriptobr.mercado.dados.Formato
import com.kriptobr.mercado.dados.Guardados
import com.kriptobr.mercado.dados.Mercado

/**
 * "Onde está mais barato agora."
 *
 * É a pergunta que todo cliente de hardware wallet faz antes de comprar, e que
 * nenhum site brasileiro responde de forma direta. A lista vem ordenada do mais
 * barato para o mais caro, com o ágio de cada corretora contra a média do
 * mercado — no Brasil essa diferença passa de 1% com frequência, o que numa
 * compra de dez mil reais é mais de cem reais.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaComparar(mercado: Mercado, aoFechar: () -> Unit) {
    val opcoes = remember(mercado.moedas) { mercado.moedas.take(30) }
    var moeda by remember(opcoes) {
        mutableStateOf(opcoes.firstOrNull { it.id == "bitcoin" } ?: opcoes.firstOrNull())
    }
    var abrirLista by remember { mutableStateOf(false) }
    var carregando by remember { mutableStateOf(false) }
    var linhas by remember { mutableStateOf<List<Corretoras.Comparacao>>(emptyList()) }
    var rodada by remember { mutableIntStateOf(0) }

    val fiat = Guardados.fiat()
    val alvo = if (fiat.equals("brl", true)) "BRL" else "USD"

    LaunchedEffect(moeda?.id, rodada) {
        val m = moeda ?: return@LaunchedEffect
        carregando = true
        linhas = emptyList()
        val dolar = mercado.acharPor("tether")?.preco?.takeIf { it > 0.5 && alvo == "BRL" }
            ?: Api.dolarEmReal()
        linhas = runCatching { Corretoras.compararTodas(m.simbolo, alvo, dolar, m.preco) }
            .getOrDefault(emptyList())
        carregando = false
    }

    Scaffold(
        containerColor = Fundo,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.comparar_titulo), color = Tinta, fontSize = 17.sp) },
                navigationIcon = {
                    Text("✕", color = Tinta2, fontSize = 17.sp,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clicavel(aoFechar).padding(14.dp))
                },
                actions = {
                    TextButton(onClick = { rodada++ }, enabled = !carregando) {
                        Text(stringResource(R.string.atualizar), color = Mint, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Fundo)
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 26.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.comparar_ajuda),
                    color = Apagado, fontSize = 12.sp, lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            }

            item {
                Box {
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .background(Superficie2)
                            .clicavel { abrirLista = true }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        moeda?.let {
                            Selo(it.simbolo, 22.dp)
                            Spacer(Modifier.width(9.dp))
                            Text(it.nome, color = Tinta, fontSize = 14.sp)
                            Spacer(Modifier.width(7.dp))
                            Text(stringResource(R.string.fonte_media_nome) + ": " + Formato.dinheiro(it.preco),
                                color = Apagado, fontSize = 11.5f.sp)
                        } ?: Text(stringResource(R.string.carregando), color = Apagado, fontSize = 14.sp)
                        Spacer(Modifier.weight(1f))
                        Text("▾", color = Apagado, fontSize = 13.sp)
                    }
                    DropdownMenu(abrirLista, onDismissRequest = { abrirLista = false },
                        modifier = Modifier.background(Superficie)) {
                        opcoes.forEach { m ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Selo(m.simbolo, 18.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(m.nome, color = Tinta, fontSize = 13.5f.sp)
                                    }
                                },
                                onClick = { moeda = m; abrirLista = false }
                            )
                        }
                    }
                }
            }

            if (carregando) item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 26.dp),
                    horizontalArrangement = Arrangement.Center
                ) { CircularProgressIndicator(color = Mint, strokeWidth = 2.dp, modifier = Modifier.size(24.dp)) }
            }

            itemsIndexed(linhas) { i, l -> LinhaCorretora(l, primeira = i == 0 && l.cotacao != null) }

            if (!carregando && linhas.isNotEmpty()) item {
                Text(
                    stringResource(R.string.comparar_rodape),
                    color = Apagado, fontSize = 11.sp, lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun LinhaCorretora(l: Corretoras.Comparacao, primeira: Boolean) {
    Cartao(
        Modifier.fillMaxWidth().padding(top = 9.dp),
        corBorda = if (primeira) Mint.copy(alpha = 0.6f) else Borda
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(l.fonte.nome, color = Tinta, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    if (l.fonte.selo.isNotEmpty()) {
                        Spacer(Modifier.width(7.dp))
                        Text(
                            l.fonte.selo, color = MintTinta, fontSize = 9.5f.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Mint)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                if (primeira) Text(
                    stringResource(R.string.comparar_mais_barato),
                    color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
                val c = l.cotacao
                if (c != null && c.convertido) Text(
                    stringResource(R.string.comparar_convertido),
                    color = Apagado, fontSize = 10.5f.sp, modifier = Modifier.padding(top = 2.dp)
                )
            }
            val c = l.cotacao
            if (c == null) {
                Text(stringResource(R.string.comparar_sem_par), color = Apagado, fontSize = 11.5f.sp)
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(Formato.dinheiroCheio(c.preco), color = Tinta,
                        fontWeight = FontWeight.Bold, fontSize = 14.5f.sp)
                    l.agio?.let { a ->
                        Text(
                            (if (a >= 0) "+" else "−") + Formato.porcento(kotlin.math.abs(a), comSeta = false),
                            color = if (a <= 0) Alta else Baixa, fontSize = 11.5f.sp
                        )
                    }
                }
            }
        }
    }
}
