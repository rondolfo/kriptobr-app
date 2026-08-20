package com.kriptobr.mercado.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptobr.mercado.R
import com.kriptobr.mercado.dados.Cadastro
import kotlinx.coroutines.launch

/* Fica junto do painel, e não no site da loja, porque o painel é publicado
   como um pacote fechado de arquivos — a página vai junto e não depende de
   ninguém lembrar de criar uma página nova no WordPress. */
private const val POLITICA = "https://mercado.kriptobr.com/privacidade.html"

/**
 * Porta de entrada: e-mail antes de usar o app.
 *
 * Duas decisões que valem explicação. A primeira: o erro de digitação vira
 * sugestão em vez de recusa — "você quis dizer @gmail.com?" recupera um cadastro
 * que a recusa seca jogaria fora. A segunda: o aceite é uma caixa que a pessoa
 * marca, nunca já marcada, porque consentimento pré-marcado não é consentimento.
 */
@Composable
fun TelaCadastro(aoEntrar: () -> Unit) {
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var aceitou by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var sugestao by remember { mutableStateOf<String?>(null) }
    var enviado by remember { mutableStateOf(false) }

    val recusaTexto: (Cadastro.Veredito.Motivo) -> String = { m ->
        when (m) {
            Cadastro.Veredito.Motivo.VAZIO -> ctx.getString(R.string.email_vazio)
            Cadastro.Veredito.Motivo.FORMATO -> ctx.getString(R.string.email_invalido)
            Cadastro.Veredito.Motivo.DESCARTAVEL -> ctx.getString(R.string.email_descartavel)
        }
    }

    val enviar: () -> Unit = enviar@{
        erro = null
        if (!aceitou) { erro = ctx.getString(R.string.precisa_aceitar); return@enviar }
        when (val v = Cadastro.conferir(email)) {
            is Cadastro.Veredito.Recusado -> { erro = recusaTexto(v.motivo); return@enviar }
            is Cadastro.Veredito.Sugestao -> { sugestao = v.corrigido; return@enviar }
            else -> Unit
        }
        enviando = true
        escopo.launch {
            val limpo = email.trim().lowercase()
            val ok = Cadastro.enviar(ctx, limpo, nome.trim())
            enviando = false
            if (ok) {
                Cadastro.guardar(ctx, limpo)
                enviado = true
            } else {
                erro = ctx.getString(R.string.cadastro_falhou)
            }
        }
    }

    Scaffold(containerColor = Fundo) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(54.dp))
            Image(
                painter = painterResource(R.drawable.logo_kriptobr),
                contentDescription = "KriptoBR",
                modifier = Modifier.height(30.dp)
            )
            Spacer(Modifier.height(30.dp))

            if (enviado) {
                ConfirmeSeuEmail(email.trim().lowercase(), aoEntrar)
                return@Column
            }

            Text(stringResource(R.string.cadastro_titulo), color = Tinta,
                fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.cadastro_texto), color = Tinta2,
                fontSize = 13.5f.sp, lineHeight = 20.sp)

            Spacer(Modifier.height(24.dp))
            CampoTexto(
                valor = email,
                aoMudar = { email = it; erro = null; sugestao = null },
                rotulo = stringResource(R.string.seu_email),
                tipo = KeyboardType.Email,
                acao = ImeAction.Next
            )
            sugestao?.let { certo ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.quis_dizer), color = Apagado, fontSize = 12.5f.sp)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        certo, color = Mint, fontSize = 12.5f.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clicavel { email = certo; sugestao = null }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            CampoTexto(
                valor = nome,
                aoMudar = { nome = it },
                rotulo = stringResource(R.string.seu_nome_opcional),
                tipo = KeyboardType.Text,
                acao = ImeAction.Done
            )

            Spacer(Modifier.height(20.dp))
            Row(Modifier.clicavel { aceitou = !aceitou; erro = null }, verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = aceitou,
                    onCheckedChange = { aceitou = it; erro = null },
                    colors = CheckboxDefaults.colors(checkedColor = Mint, checkmarkColor = MintTinta,
                        uncheckedColor = Borda)
                )
                Spacer(Modifier.width(4.dp))
                Column(Modifier.padding(top = 13.dp)) {
                    Text(stringResource(R.string.aceite), color = Tinta2, fontSize = 12.5f.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.ler_privacidade), color = Mint,
                        fontSize = 12.5f.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clicavel { abrirLink(ctx, POLITICA) }
                    )
                }
            }

            erro?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Baixa, fontSize = 12.5f.sp, lineHeight = 17.sp)
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = enviar,
                enabled = !enviando,
                colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = MintTinta,
                    disabledContainerColor = Superficie2, disabledContentColor = Apagado),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (enviando) CircularProgressIndicator(color = MintTinta, strokeWidth = 2.dp,
                    modifier = Modifier.height(20.dp).width(20.dp))
                else Text(stringResource(R.string.entrar_no_app), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.cadastro_rodape), color = Apagado,
                fontSize = 11.5f.sp, lineHeight = 16.sp)
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ConfirmeSeuEmail(email: String, aoEntrar: () -> Unit) {
    val ctx = LocalContext.current
    Text(stringResource(R.string.confirme_titulo), color = Tinta,
        fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 29.sp)
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.confirme_texto, email), color = Tinta2,
        fontSize = 13.5f.sp, lineHeight = 20.sp)
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = { abrirCaixaDeEntrada(ctx) },
        colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = MintTinta),
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) { Text(stringResource(R.string.abrir_email), fontWeight = FontWeight.Bold, fontSize = 15.sp) }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = aoEntrar,
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) { Text(stringResource(R.string.ja_confirmei), color = Tinta2, fontSize = 14.sp) }
    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.confirme_rodape), color = Apagado, fontSize = 11.5f.sp, lineHeight = 16.sp)
    Spacer(Modifier.height(40.dp))
}

@Composable
private fun CampoTexto(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    tipo: KeyboardType,
    acao: ImeAction
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        singleLine = true,
        label = { Text(rotulo, color = Apagado, fontSize = 13.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = tipo, imeAction = acao),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Mint, unfocusedBorderColor = Borda,
            focusedTextColor = Tinta, unfocusedTextColor = Tinta,
            cursorColor = Mint,
            focusedContainerColor = Superficie, unfocusedContainerColor = Superficie
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun abrirLink(ctx: android.content.Context, url: String) {
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Leva a pessoa para o aplicativo de e-mail dela, seja qual for. */
private fun abrirCaixaDeEntrada(ctx: android.content.Context) {
    val i = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_APP_EMAIL)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { ctx.startActivity(i) }
}
