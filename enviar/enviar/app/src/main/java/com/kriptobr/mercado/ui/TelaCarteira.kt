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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptobr.mercado.R
import com.kriptobr.mercado.dados.Api
import com.kriptobr.mercado.dados.Carteira
import com.kriptobr.mercado.dados.Formato
import com.kriptobr.mercado.dados.Guardados
import com.kriptobr.mercado.dados.Mercado
import com.kriptobr.mercado.dados.Moeda
import com.kriptobr.mercado.dados.Posicao

/**
 * Minha carteira: quanto a pessoa tem e por quanto comprou.
 *
 * Tudo digitado à mão e guardado só no aparelho. Nada de chave de API, nada de
 * conectar corretora — o app é de uma revendedora de hardware wallet, e pedir a
 * credencial da exchange do cliente seria o oposto exato do que a KriptoBR
 * vende. O aviso no rodapé diz isso com todas as letras, porque é diferencial.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCarteira(mercado: Mercado, aoFechar: () -> Unit) {
    val ctx = LocalContext.current
    var posicoes by remember { mutableStateOf(Carteira.tudo(ctx)) }
    var editando by remember { mutableStateOf<Posicao?>(null) }
    var criando by remember { mutableStateOf(false) }

    val fiat = Guardados.fiat()
    /* Câmbio do dólar em real. Com a tela em real o preço do tether já serve e
       não custa requisição nenhuma; com a tela em dólar o tether vale ~1, que
       não é câmbio — aí precisa buscar de fato. Só entra em cena quando a
       posição foi digitada numa moeda diferente da que a tela mostra. */
    var dolar by remember { mutableDoubleStateOf(0.0) }
    LaunchedEffect(fiat, mercado.atualizadoEm) {
        dolar = if (fiat.equals("brl", true)) {
            mercado.acharPor("tether")?.preco?.takeIf { it > 0.5 } ?: Api.dolarEmReal()
        } else {
            Api.dolarEmReal()
        }
    }
    val resumo = remember(posicoes, mercado, dolar) { Carteira.calcular(posicoes, mercado, fiat, dolar) }

    fun gravar(lista: List<Posicao>) {
        posicoes = lista
        Carteira.salvar(ctx, lista)
    }

    Scaffold(
        containerColor = Fundo,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.carteira_titulo), color = Tinta, fontSize = 17.sp) },
                navigationIcon = {
                    Text("✕", color = Tinta2, fontSize = 17.sp,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clicavel(aoFechar).padding(14.dp))
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
                Cartao(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(stringResource(R.string.carteira_hoje), color = Apagado, fontSize = 11.5f.sp)
                    Text(
                        Formato.dinheiroCheio(resumo.valorHoje),
                        color = Tinta, fontWeight = FontWeight.Bold, fontSize = 27.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Variacao(resumo.variacao)
                        Spacer(Modifier.width(9.dp))
                        Text(
                            (if (resumo.lucro >= 0) "+" else "−") +
                                Formato.dinheiroCheio(kotlin.math.abs(resumo.lucro)),
                            color = if (resumo.lucro >= 0) Alta else Baixa,
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.carteira_investido, Formato.dinheiroCheio(resumo.investido)),
                        color = Apagado, fontSize = 11.5f.sp
                    )
                    if (resumo.semPreco > 0) Text(
                        stringResource(R.string.carteira_sem_preco, resumo.semPreco),
                        color = Apagado, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }

            item {
                Button(
                    onClick = { criando = true },
                    modifier = Modifier.fillMaxWidth().height(46.dp).padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = MintTinta)
                ) { Text(stringResource(R.string.carteira_adicionar), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            }

            items(posicoes, key = { it.id }) { pos ->
                LinhaPosicao(
                    pos, mercado.acharPor(pos.id), fiat, dolar,
                    aoEditar = { editando = pos },
                    aoRemover = { gravar(posicoes.filterNot { it.id == pos.id }) }
                )
            }

            if (posicoes.isEmpty()) item {
                Text(
                    stringResource(R.string.carteira_vazia),
                    color = Apagado, fontSize = 12.5f.sp, lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 18.dp)
                )
            }

            item {
                Cartao(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    corBorda = Mint.copy(alpha = 0.5f)
                ) {
                    Text(stringResource(R.string.carteira_privacidade_titulo), color = Tinta,
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        stringResource(R.string.carteira_privacidade), color = Tinta2,
                        fontSize = 12.5f.sp, lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }

    if (criando || editando != null) {
        DialogoPosicao(
            inicial = editando,
            opcoes = mercado.moedas,
            fiat = fiat,
            aoFechar = { criando = false; editando = null },
            aoSalvar = { nova ->
                gravar(posicoes.filterNot { it.id == nova.id } + nova)
                criando = false; editando = null
            }
        )
    }
}

@Composable
private fun LinhaPosicao(
    pos: Posicao,
    atual: Moeda?,
    fiat: String,
    dolar: Double,
    aoEditar: () -> Unit,
    aoRemover: () -> Unit
) {
    val valor = atual?.let { pos.quantidade * it.preco }
    val fator = when {
        pos.moeda.equals(fiat, true) -> 1.0
        dolar <= 0.0 -> 0.0
        pos.moeda.equals("usd", true) -> dolar
        else -> 1.0 / dolar
    }
    val custo = pos.quantidade * pos.precoMedio * fator
    val lucro = if (valor != null && fator > 0.0) valor - custo else null

    Cartao(Modifier.fillMaxWidth().padding(top = 9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Selo(pos.simbolo, 26.dp)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f).clicavel(aoEditar)) {
                Text(pos.nome, color = Tinta, fontWeight = FontWeight.SemiBold, fontSize = 13.5f.sp)
                Text(
                    Formato.numero(pos.quantidade, 8) + " " + pos.simbolo,
                    color = Apagado, fontSize = 11.5f.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    valor?.let { Formato.dinheiroCheio(it) } ?: stringResource(R.string.travessao),
                    color = Tinta, fontWeight = FontWeight.Bold, fontSize = 13.5f.sp
                )
                lucro?.let {
                    Text(
                        (if (it >= 0) "+" else "−") + Formato.dinheiroCheio(kotlin.math.abs(it)),
                        color = if (it >= 0) Alta else Baixa, fontSize = 11.5f.sp
                    )
                }
            }
            Text("✕", color = Apagado, fontSize = 15.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clicavel(aoRemover).padding(6.dp))
        }
    }
}

@Composable
private fun DialogoPosicao(
    inicial: Posicao?,
    opcoes: List<Moeda>,
    fiat: String,
    aoFechar: () -> Unit,
    aoSalvar: (Posicao) -> Unit
) {
    var moeda by remember {
        mutableStateOf(
            opcoes.firstOrNull { it.id == inicial?.id }
                ?: opcoes.firstOrNull { it.id == "bitcoin" }
                ?: opcoes.firstOrNull()
        )
    }
    var abrirLista by remember { mutableStateOf(false) }
    var quantidade by remember { mutableStateOf(inicial?.let { Formato.numero(it.quantidade, 8) } ?: "") }
    var medio by remember { mutableStateOf(inicial?.let { Formato.numero(it.precoMedio, 2) } ?: "") }

    fun numero(t: String) = t.replace(".", "").replace(",", ".").toDoubleOrNull()
    val qtd = numero(quantidade)
    val pm = numero(medio)
    val pode = moeda != null && qtd != null && qtd > 0.0

    AlertDialog(
        onDismissRequest = aoFechar,
        containerColor = Superficie,
        title = {
            Text(
                stringResource(if (inicial == null) R.string.carteira_nova else R.string.carteira_editar),
                color = Tinta, fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
        },
        text = {
            Column {
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
                            Selo(it.simbolo, 20.dp)
                            Spacer(Modifier.width(9.dp))
                            Text(it.nome, color = Tinta, fontSize = 14.sp)
                        } ?: Text(stringResource(R.string.carregando), color = Apagado, fontSize = 14.sp)
                        Spacer(Modifier.weight(1f))
                        Text("▾", color = Apagado, fontSize = 13.sp)
                    }
                    DropdownMenu(abrirLista, onDismissRequest = { abrirLista = false },
                        modifier = Modifier.background(Superficie)) {
                        opcoes.take(40).forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.nome, color = Tinta, fontSize = 13.5f.sp) },
                                onClick = { moeda = m; abrirLista = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                CampoNumero(quantidade, stringResource(R.string.carteira_quantidade)) { quantidade = it }
                Spacer(Modifier.height(10.dp))
                CampoNumero(medio, stringResource(R.string.carteira_preco_medio)) { medio = it }
                Text(
                    stringResource(R.string.carteira_moeda_digitada, fiat.uppercase()),
                    color = Apagado, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = pode,
                onClick = {
                    val m = moeda ?: return@TextButton
                    aoSalvar(
                        Posicao(
                            id = m.id, simbolo = m.simbolo, nome = m.nome,
                            quantidade = qtd ?: 0.0, precoMedio = pm ?: 0.0, moeda = fiat
                        )
                    )
                }
            ) { Text(stringResource(R.string.salvar), color = if (pode) Mint else Apagado, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = aoFechar) { Text(stringResource(R.string.cancelar), color = Apagado) }
        }
    )
}

@Composable
private fun CampoNumero(valor: String, rotulo: String, aoMudar: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = { t -> aoMudar(t.filter { it.isDigit() || it == '.' || it == ',' }) },
        label = { Text(rotulo, color = Apagado, fontSize = 12.sp) },
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
}
