package com.kriptobr.mercado.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                Text(stringResource(R.string.editar_ajuda), color = Apagado,
                    fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(bottom = 10.dp))
            }
            items(catalogo, key = { it.id }) { m ->
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
