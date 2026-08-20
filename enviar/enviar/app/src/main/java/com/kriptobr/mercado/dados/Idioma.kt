package com.kriptobr.mercado.dados

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Idioma do app.
 *
 * Por padrão o Android manda: quem tem o celular em inglês vê o app em inglês.
 * Só que muita gente usa o aparelho em inglês e prefere ler em português — então
 * a escolha fica com a pessoa, e não com o sistema.
 *
 * A escolha vale para o processo inteiro (tela, widget e o verificador de
 * alertas que roda em segundo plano), porque quem aplica é o [com.kriptobr.mercado.App],
 * que sempre roda antes de qualquer um deles.
 */
object Idioma {

    const val AUTO = "auto"
    val SUPORTADOS = listOf("pt", "en", "es")

    private const val ARQ = "kriptobr"
    private const val K = "idioma"

    /** Lido uma vez por processo; evita abrir o arquivo de preferências a cada formatação. */
    @Volatile private var escolhaEmMemoria: String? = null

    private fun p(ctx: Context) = ctx.getSharedPreferences(ARQ, Context.MODE_PRIVATE)

    fun escolha(ctx: Context): String {
        escolhaEmMemoria?.let { return it }
        val v = p(ctx).getString(K, AUTO) ?: AUTO
        escolhaEmMemoria = v
        return v
    }

    fun salvar(ctx: Context, tag: String) {
        val limpo = if (tag in SUPORTADOS) tag else AUTO
        escolhaEmMemoria = limpo
        p(ctx).edit().putString(K, limpo).apply()
        aplicarNoProcesso(ctx)
    }

    /** Código de duas letras que aparece no botão: sempre o idioma que está valendo. */
    fun atual(ctx: Context): String {
        val e = escolha(ctx)
        if (e != AUTO) return e
        val doAparelho = Locale.getDefault().language.lowercase()
        return if (doAparelho in SUPORTADOS) doAparelho else "en"
    }

    private fun localeDe(tag: String): Locale =
        if (tag == "pt") Locale("pt", "BR") else Locale(tag)

    /** Chamado no início do processo: faz Locale.getDefault() concordar com a escolha. */
    fun aplicarNoProcesso(ctx: Context) {
        val e = escolha(ctx)
        Guardados.idiomaValendo = if (e == AUTO) "" else e
        if (e != AUTO) Locale.setDefault(localeDe(e))
    }

    /**
     * Envolve o contexto da tela para os textos saírem no idioma escolhido.
     * Usado no attachBaseContext da Activity — é lá que o Android decide de qual
     * pasta values-* vai ler as frases.
     */
    fun envolver(base: Context): Context {
        val e = escolha(base)
        if (e == AUTO) return base
        val loc = localeDe(e)
        Guardados.idiomaValendo = e
        Locale.setDefault(loc)
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(loc)
        return base.createConfigurationContext(cfg)
    }
}
