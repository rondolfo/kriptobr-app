package com.kriptobr.mercado.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptobr.mercado.R
import com.kriptobr.mercado.alerta.Alerta
import com.kriptobr.mercado.dados.Formato
import com.kriptobr.mercado.dados.Mercado
import com.kriptobr.mercado.dados.Moeda
import kotlin.math.abs

@Composable
fun TelaAlertas(
    mercado: Mercado,
    alertas: List<Alerta>,
    aoCriar: (Alerta) -> Unit,
    aoAlternar: (Alerta) -> Unit,
    aoRemover: (Alerta) -> Unit
) {
    val opcoes = mercado.moedas.ifEmpty { emptyList() }
    var escolhida by remember(opcoes) { mutableStateOf(opcoes.firstOrNull { it.id == "bitcoin" } ?: opcoes.firstOrNull()) }
    var acima by remember { mutableStateOf(true) }
    var valor by remember { mutableStateOf("") }
    var abrirLista by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        item {
            Cartao(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Rotulo(stringResource(R.string.moeda))
                Box {
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .background(Superficie2)
                            .clicavel { abrirLista = true }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        escolhida?.let {
                            Selo(it.simbolo, 20.dp)
                            Spacer(Modifier.width(9.dp))
                            Text(it.nome, color = Tinta, fontSize = 14.sp)
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
                                onClick = { escolhida = m; abrirLista = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Rotulo(stringResource(R.string.me_avise_quando))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Superficie2)
                ) {
                    Aba(stringResource(R.string.acima_de), acima, Modifier.weight(1f)) { acima = true }
                    Aba(stringResource(R.string.abaixo_de), !acima, Modifier.weight(1f)) { acima = false }
                }

                Spacer(Modifier.height(12.dp))
                Rotulo(stringResource(R.string.valor))
                OutlinedTextField(
                    value = valor,
                    onValueChange = { t -> valor = t.filter { it.isDigit() || it == '.' || it == ',' } },
                    placeholder = { Text(exemploValor(escolhida), color = Apagado) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(11.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Superficie2, unfocusedContainerColor = Superficie2,
                        focusedBorderColor = Mint, unfocusedBorderColor = Borda,
                        focusedTextColor = Tinta, unfocusedTextColor = Tinta, cursorColor = Mint
                    )
                )

                val alvo = valor.replace(".", "").replace(",", ".").toDoubleOrNull()
                val podeCriar = escolhida != null && alvo != null && alvo > 0
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val m = escolhida ?: return@Button
                        val a = alvo ?: return@Button
                        aoCriar(
                            Alerta(
                                id = System.currentTimeMillis(),
                                moedaId = m.id, simbolo = m.simbolo, nome = m.nome,
                                acima = acima, alvo = a
                            )
                        )
                        valor = ""
                    },
                    enabled = podeCriar,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Mint, contentColor = MintTinta,
                        disabledContainerColor = Superficie2, disabledContentColor = Apagado
                    )
                ) { Text(stringResource(R.string.criar_alerta), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            }
        }

        item {
            TituloSecao(
                stringResource(R.string.meus_alertas),
                if (alertas.isEmpty()) null else stringResource(R.string.n_ativos, alertas.count { it.ativo })
            )
        }

        items(alertas, key = { it.id }) { a ->
            LinhaAlerta(a, mercado.acharPor(a.moedaId), { aoAlternar(a) }, { aoRemover(a) })
        }

        item {
            Cartao(
                Modifier.fillMaxWidth().padding(top = if (alertas.isEmpty()) 4.dp else 12.dp),
                corBorda = Mint.copy(alpha = 0.55f)
            ) {
                Text(stringResource(R.string.como_funciona), color = Tinta,
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    stringResource(R.string.como_funciona_texto), color = Tinta2,
                    fontSize = 12.5f.sp, lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun Rotulo(texto: String) {
    Text(texto, color = Apagado, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 5.dp))
}

@Composable
private fun Aba(texto: String, ligada: Boolean, modifier: Modifier, aoTocar: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (ligada) Mint else androidx.compose.ui.graphics.Color.Transparent)
            .clicavel(aoTocar)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, color = if (ligada) MintTinta else Tinta2,
            fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun LinhaAlerta(a: Alerta, atual: Moeda?, aoAlternar: () -> Unit, aoRemover: () -> Unit) {
    Cartao(Modifier.fillMaxWidth().padding(bottom = 9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Selo(a.simbolo, 26.dp)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(
                        if (a.acima) R.string.alerta_acima else R.string.alerta_abaixo,
                        a.simbolo, Formato.dinheiro(a.alvo)
                    ),
                    color = Tinta, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
                Text(subtitulo(a, atual), color = Apagado, fontSize = 11.sp)
            }
            Switch(
                checked = a.ativo, onCheckedChange = { aoAlternar() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                    checkedTrackColor = Mint,
                    uncheckedThumbColor = Tinta2, uncheckedTrackColor = Superficie2,
                    uncheckedBorderColor = Borda
                )
            )
            Spacer(Modifier.width(4.dp))
            Text("✕", color = Apagado, fontSize = 15.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clicavel(aoRemover).padding(6.dp))
        }
    }
}

@Composable
private fun subtitulo(a: Alerta, atual: Moeda?): String {
    if (!a.ativo) return stringResource(R.string.pausado)
    if (a.disparadoEm > 0) return stringResource(R.string.ja_disparou)
    val preco = atual?.preco ?: return stringResource(R.string.aguardando_preco)
    val falta = abs(a.alvo - preco) / preco * 100
    return stringResource(R.string.faltam_pct, Formato.porcento(falta, comSeta = false))
}

private fun exemploValor(m: Moeda?): String =
    if (m == null) "0,00" else Formato.numero(m.preco * 1.1, 2)
