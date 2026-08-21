package com.kriptobr.mercado.dados

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.random.Random

/**
 * Cadastro do visitante, com confirmação de que o e-mail é mesmo dele.
 *
 * Como a confirmação funciona, já que não temos servidor: o aplicativo sorteia
 * um código de seis dígitos e pede ao Jotform que o envie para o endereço
 * digitado. Só quem abre aquela caixa de entrada lê o código. Digitou certo,
 * está provado que o endereço existe e é dele — e aí, e só aí, o cadastro vai
 * para a lista de verdade. Endereço errado ou inventado nunca chega lá.
 *
 * São dois formulários de propósito: um só serve de carteiro para o código, o
 * outro guarda a lista limpa. Assim a lista de cupons não mistura tentativa com
 * cadastro confirmado.
 */
object Cadastro {

    /** Formulário que guarda a lista confirmada. */
    private const val FORM_LISTA = "262317031759053"
    /** Formulário que só dispara o e-mail com o código. */
    private const val FORM_CODIGO = "262317840357055"

    private const val ENVIO = "https://submit.jotform.com/submit/"
    const val PAGINA_LISTA = "https://form.jotform.com/$FORM_LISTA"

    private const val ARQ = "kriptobr"
    private const val K_EMAIL = "cadastro_email"
    private const val K_QUANDO = "cadastro_quando"
    private const val K_PRIMEIRO_USO = "primeiro_uso"
    private const val K_ADIADO = "cadastro_adiado"
    private const val K_CODIGO = "cadastro_codigo"
    private const val K_CODIGO_QUANDO = "cadastro_codigo_quando"
    private const val K_CODIGO_EMAIL = "cadastro_codigo_email"

    /** Só depois de um dia de uso. Quem acabou de instalar quer ver o preço, não um formulário. */
    private const val ESPERA_INICIAL = 24L * 3600 * 1000
    /** "Agora não" empurra por uma semana. */
    private const val ESPERA_ADIADO = 7L * 24 * 3600 * 1000
    /** O código morre em meia hora. */
    private const val VALIDADE_CODIGO = 30L * 60 * 1000

    private fun p(ctx: Context) = ctx.getSharedPreferences(ARQ, Context.MODE_PRIVATE)

    // ------------------------------------------------------------ quando aparecer

    /** Chamado toda vez que o app abre; grava a data da primeira vez e só dela. */
    fun marcarUso(ctx: Context) {
        if (p(ctx).getLong(K_PRIMEIRO_USO, 0L) == 0L) {
            p(ctx).edit().putLong(K_PRIMEIRO_USO, System.currentTimeMillis()).apply()
        }
    }

    fun jaCadastrado(ctx: Context): Boolean = !p(ctx).getString(K_EMAIL, null).isNullOrBlank()
    fun emailGuardado(ctx: Context): String = p(ctx).getString(K_EMAIL, "") ?: ""

    fun deveMostrar(ctx: Context): Boolean {
        if (jaCadastrado(ctx)) return false
        val agora = System.currentTimeMillis()
        val primeiro = p(ctx).getLong(K_PRIMEIRO_USO, agora)
        if (agora - primeiro < ESPERA_INICIAL) return false
        val adiado = p(ctx).getLong(K_ADIADO, 0L)
        return agora - adiado >= ESPERA_ADIADO
    }

    fun adiar(ctx: Context) {
        p(ctx).edit().putLong(K_ADIADO, System.currentTimeMillis()).apply()
    }

    fun guardar(ctx: Context, email: String) {
        p(ctx).edit()
            .putString(K_EMAIL, email)
            .putLong(K_QUANDO, System.currentTimeMillis())
            .remove(K_CODIGO).remove(K_CODIGO_QUANDO).remove(K_CODIGO_EMAIL)
            .apply()
    }

    /** Apaga o cadastro do aparelho. A saída da lista é pedida pelo e-mail de contato. */
    fun esquecer(ctx: Context) {
        p(ctx).edit().remove(K_EMAIL).remove(K_QUANDO).apply()
    }

    // ------------------------------------------------------------ validação do texto

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

    // ------------------------------------------------------------ código

    private fun sortearCodigo(): String = (100000 + Random.nextInt(900000)).toString()

    /** Sorteia o código, guarda no aparelho e pede ao Jotform que o envie. */
    suspend fun enviarCodigo(ctx: Context, email: String): Boolean {
        val codigo = sortearCodigo()
        val ok = postar(FORM_CODIGO, listOf(
            "formID" to FORM_CODIGO,
            "q2_q2_email0" to email,
            "q3_q3_textbox1" to codigo,
            "simple_spc" to "$FORM_CODIGO-$FORM_CODIGO"
        ))
        if (ok) {
            p(ctx).edit()
                .putString(K_CODIGO, codigo)
                .putString(K_CODIGO_EMAIL, email)
                .putLong(K_CODIGO_QUANDO, System.currentTimeMillis())
                .apply()
        }
        return ok
    }

    enum class Conferencia { CERTO, ERRADO, EXPIRADO }

    fun conferirCodigo(ctx: Context, digitado: String): Conferencia {
        val guardado = p(ctx).getString(K_CODIGO, null) ?: return Conferencia.EXPIRADO
        val quando = p(ctx).getLong(K_CODIGO_QUANDO, 0L)
        if (System.currentTimeMillis() - quando > VALIDADE_CODIGO) return Conferencia.EXPIRADO
        return if (digitado.trim() == guardado) Conferencia.CERTO else Conferencia.ERRADO
    }

    // ------------------------------------------------------------ lista final

    /** Só depois do código conferido. É esta lista que recebe cupom. */
    suspend fun entrarNaLista(ctx: Context, email: String, nome: String): Boolean {
        val origem = "Android ${Build.VERSION.RELEASE} · app ${versao(ctx)}"
        return postar(FORM_LISTA, listOf(
            "formID" to FORM_LISTA,
            "q2_q2_email0" to email,
            "q3_q3_textbox1" to nome,
            "q4_q4_textbox2" to origem,
            "q5_q5_widget_TermsAndConditions3" to "Aceito",
            "simple_spc" to "$FORM_LISTA-$FORM_LISTA"
        ))
    }

    // ------------------------------------------------------------ transporte

    private fun campo(nome: String, valor: String) =
        URLEncoder.encode(nome, "UTF-8") + "=" + URLEncoder.encode(valor, "UTF-8")

    private suspend fun postar(formId: String, dados: List<Pair<String, String>>): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val corpo = dados.joinToString("&") { campo(it.first, it.second) }
                val con = (URL(ENVIO + formId).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 20000
                    doOutput = true
                    instanceFollowRedirects = false
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    setRequestProperty("User-Agent", "KriptoBRApp/2.1")
                }
                try {
                    OutputStreamWriter(con.outputStream, "UTF-8").use { it.write(corpo) }
                    con.responseCode in 200..399   // o Jotform responde 200 ou redireciona
                } finally {
                    con.disconnect()
                }
            }.getOrDefault(false)
        }

    private fun versao(ctx: Context): String = runCatching {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
    }.getOrDefault("?")
}
