package com.kriptobr.mercado.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptobr.mercado.R
import com.kriptobr.mercado.dados.Corretoras

/**
 * De onde vem o preço.
 *
 * A média do mercado é o número que todo site mostra, mas não é o preço que
 * ninguém paga. Quem compra na Mercado Bitcoin quer ver o preço da Mercado
 * Bitcoin — no Brasil o ágio costuma passar de 1%, e isso muda a conta.
 */
@Composable
fun DialogoFonte(
    atual: String,
    aoEscolher: (String) -> Unit,
    aoFechar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = aoFechar,
        containerColor = Superficie,
        title = {
            Column {
                Text(
                    stringResource(R.string.fonte_titulo),
                    color = Tinta, fontWeight = FontWeight.Bold, fontSize = 17.sp
                )
                Text(
                    stringResource(R.string.fonte_explica),
                    color = Apagado, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Corretoras.LISTA.forEach { f ->
                    OpcaoFonte(
                        nome = if (f.id == Corretoras.MEDIA) stringResource(R.string.fonte_media_nome) else f.nome,
                        selo = f.selo,
                        marcada = f.id == atual
                    ) { aoEscolher(f.id) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = aoFechar) {
                Text(stringResource(R.string.fechar), color = Mint, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun OpcaoFonte(nome: String, selo: String, marcada: Boolean, aoTocar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clicavel(aoTocar)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = marcada,
            onClick = aoTocar,
            colors = RadioButtonDefaults.colors(selectedColor = Mint, unselectedColor = Apagado)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            nome,
            color = if (marcada) Tinta else Tinta2,
            fontWeight = if (marcada) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.5f.sp,
            modifier = Modifier.weight(1f)
        )
        if (selo.isNotEmpty()) {
            Text(
                selo,
                color = MintTinta,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Mint)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
