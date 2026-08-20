package com.kriptobr.mercado.dados

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cadastro do visitante: e-mail, aceite e envio para o formulário do Jotform da
 * KriptoBR — que é quem guarda a lista e dispara a confirmação.
 *
 * A validação aqui é local e serve para dois problemas diferentes: barrar o que
 * nem é e-mail, e — principalmente — pegar o erro de digitação, que é o que mais
 * suja lista de e-mail na prática. "gmial.com" passa em qualquer regex e nunca
 * recebe nada.
 */
object Cadastro {

    private const val FORM_ID = "262317031759053"
    private const val ENVIO = "https://submit.jotform.com/submit/$FORM_ID"
    const val PAGINA = "https://form.jotform.com/$FORM_ID"

    private const val ARQ = "kriptobr"
    private const val K_EMAIL = "cadastro_email"
    private const val K_QUANDO = "cadastro_quando"

    private fun p(ctx: Context) = ctx.getSharedPreferences(ARQ, Context.MODE_PRIVATE)

    fun jaCadastrado(ctx: Context): Boolean = !p(ctx).getString(K_EMAIL, null).isNullOrBlank()
    fun emailGuardado(ctx: Context): String = p(ctx).getString(K_EMAIL, "") ?: ""

    fun guardar(ctx: Context, email: String) {
        p(ctx).edit()
            .putString(K_EMAIL, email)
            .putLong(K_QUANDO, System.currentTimeMillis())
            .apply()
    }

    /** Apaga o cadastro do aparelho. A remoção da lista é pedida pelo e-mail de contato. */
    fun esquecer(ctx: Context) {
        p(ctx).edit().remove(K_EMAIL).remove(K_QUANDO).apply()
    }

    // ---------------------------------------------------------------- validação

    private val FORMATO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,24}$")

    /** Domínios de e-mail descartável: quem usa não quer receber, e suja a lista. */
    private val DESCARTAVEIS = setOf(
        "mailinator.com", "tempmail.com", "temp-mail.org", "10minutemail.com",
        "guerrillamail.com", "yopmail.com", "trashmail.com", "sharklasers.com",
        "getnada.com", "dispostable.com", "fakeinbox.com", "maildrop.cc"
    )

    /** Erros de digitação comuns nos domínios grandes do Brasil. */
    private val TROPECOS = mapOf(
        "gmial.com" to "gmail.com", "gmai.com" to "gmail.com", "gamil.com" to "gmail.com",
        "gmail.co" to "gmail.com", "gmail.con" to "gmail.com", "gnail.com" to "gmail.com",
        "hotmial.com" to "hotmail.com", "hotmail.co" to "hotmail.com", "hotmai.com" to "hotmail.com",
        "hotmail.con" to "hotmail.com", "homtail.com" to "hotmail.com",
        "outlok.com" to "outlook.com", "outllok.com" to "outlook.com", "outlook.co" to "outlook.com",
        "yaho.com" to "yahoo.com", "yahooo.com" to "yahoo.com", "yahoo.co" to "yahoo.com",
        "iclod.com" to "icloud.com", "icloud.co" to "icloud.com",
        "bol.com" to "bol.com.br", "uol.com" to "uol.com.br", "terra.com" to "terra.com.br"
    )

    sealed class Veredito {
        object Ok : Veredito()
        data class Sugestao(val corrigido: String) : Veredito()
        data class Recusado(val motivo: Motivo) : Veredito()
        enum class Motivo { VAZIO, FORMATO, DESCARTAVEL }
    }

    fun conferir(bruto: String): Veredito {
        val e = bruto.trim().lowercase()
        if (e.isEmpty()) return Veredito.Recusado(Veredito.Motivo.VAZIO)
        if (!FORMATO.matches(e)) return Veredito.Recusado(Veredito.Motivo.FORMATO)
        val dominio = e.substringAfterLast('@')
        if (dominio in DESCARTAVEIS) return Veredito.Recusado(Veredito.Motivo.DESCARTAVEL)
        TROPECOS[dominio]?.let { certo ->
            return Veredito.Sugestao(e.substringBeforeLast('@') + "@" + certo)
        }
        return Veredito.Ok
    }

    // ---------------------------------------------------------------- envio

    private fun campo(nome: String, valor: String) =
        URLEncoder.encode(nome, "UTF-8") + "=" + URLEncoder.encode(valor, "UTF-8")

    /**
     * Manda para o Jotform no mesmo formato que o navegador mandaria. Devolve
     * true quando o formulário aceitou; o e-mail de confirmação sai de lá.
     */
    suspend fun enviar(ctx: Context, email: String, nome: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val origem = "Android ${Build.VERSION.RELEASE} · app ${versao(ctx)}"
                val corpo = listOf(
                    campo("formID", FORM_ID),
                    campo("q2_q2_email0", email),
                    campo("q3_q3_textbox1", nome),
                    campo("q4_q4_textbox2", origem),
                    campo("q5_q5_widget_TermsAndConditions3", "Aceito"),
                    campo("simple_spc", "$FORM_ID-$FORM_ID")
                ).joinToString("&")

                val con = (URL(ENVIO).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 20000
                    doOutput = true
                    instanceFollowRedirects = false
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    setRequestProperty("User-Agent", "KriptoBRApp/2.0")
                }
                try {
                    OutputStreamWriter(con.outputStream, "UTF-8").use { it.write(corpo) }
                    // o Jotform responde 200 ou redireciona para a página de obrigado
                    con.responseCode in 200..399
                } finally {
                    con.disconnect()
                }
            }.getOrDefault(false)
        }

    private fun versao(ctx: Context): String = runCatching {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
    }.getOrDefault("?")
}
