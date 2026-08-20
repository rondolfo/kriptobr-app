package com.kriptobr.mercado.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptobr.mercado.R
import com.kriptobr.mercado.dados.Formato
import com.kriptobr.mercado.dados.Moeda

/** Escolha das moedas que aparecem em "Minha lista". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaEditarLista(
    catalogo: List<Moeda>,
    favoritos: List<String>,
    carregando: Boolean,
    aoAlternar: (String) -> Unit,
    aoFechar: () -> Unit
) {
    Scaffold(
        containerColor = Fundo,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.minha_lista), color = Tinta, fontSize = 17.sp) },
                navigationIcon = {
                    Text("✕", color = Tinta2, fontSize = 17.sp,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clicavel(aoFechar).padding(14.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Fundo)
            )
        }
    ) { pad ->
        if (carregando && catalogo.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Mint)
            }
            return@Scaffold
        }
        /* Com 250 moedas, rolar até achar a sua é trabalho. A busca resolve, e
           filtrar em memória é instantâneo — nada aqui vai à internet. */
        var busca by remember { mutableStateOf("") }
        val visiveis = remember(busca, catalogo) {
            val q = busca.trim().lowercase()
            if (q.isEmpty()) catalogo
            else catalogo.filter { it.nome.lowercase().contains(q) || it.simbolo.lowercase().contains(q) }
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                Text(stringResource(R.string.editar_ajuda), color = Apagado,
                    fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(bottom = 10.dp))
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.buscar_moeda), color = Apagado, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Mint, unfocusedBorderColor = Borda,
                        focusedTextColor = Tinta, unfocusedTextColor = Tinta,
                        cursorColor = Mint,
                        focusedContainerColor = Superficie, unfocusedContainerColor = Superficie
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )
                Text(
                    stringResource(R.string.n_escolhidas, favoritos.size),
                    color = Mint, fontSize = 11.5f.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, top = 2.dp)
                )
            }
            if (visiveis.isEmpty()) item {
                Text(stringResource(R.string.nada_encontrado), color = Apagado, fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 24.dp))
            }
            items(visiveis, key = { it.id }) { m ->
                val marcada = m.id in favoritos
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clicavel { aoAlternar(m.id) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Selo(m.simbolo, 26.dp)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.nome, color = Tinta, fontSize = 13.5f.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text("${m.simbolo} · ${Formato.dinheiro(m.preco)}", color = Apagado, fontSize = 11.sp)
                    }
                    Box(
                        Modifier.size(24.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (marcada) Mint else Superficie2),
                        contentAlignment = Alignment.Center
                    ) {
                        if (marcada) Text("✓", color = MintTinta, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(color = Borda.copy(alpha = 0.45f))
            }
        }
    }
}
