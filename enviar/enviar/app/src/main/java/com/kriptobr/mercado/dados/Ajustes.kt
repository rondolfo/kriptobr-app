package com.kriptobr.mercado.dados

import android.content.Context
import java.util.Calendar

/**
 * Preferências que o usuário controla na tela de ajustes: moeda, widget e avisos.
 *
 * Ficam separadas de [Guardados] de propósito — lá é o depósito de dados
 * (favoritos, alertas, cache de preço); aqui é só escolha de gosto.
 */
object Ajustes {

    private const val ARQ = "kriptobr"

    private const val K_MOEDA = "moeda"
    private const val K_WIDGET_QTD = "widget_qtd"
    private const val K_WIDGET_FAIXA = "widget_faixa"
    private const val K_WIDGET_VOLUME = "widget_volume"
    private const val K_AVISO_NOTICIAS = "aviso_noticias"
    private const val K_AVISO_RESUMO = "aviso_resumo"
    private const val K_ULTIMO_RESUMO = "ultimo_resumo"
    private const val K_ULTIMA_NOTICIA_AVISADA = "ultima_noticia_avisada"

    const val AUTO = "auto"

    private fun p(ctx: Context) = ctx.getSharedPreferences(ARQ, Context.MODE_PRIVATE)

    // ---------------------------------------------------------------- moeda
    /**
     * "auto" (segue o idioma), "brl" ou "usd".
     *
     * Antes a moeda era amarrada ao idioma: quem lia em inglês não conseguia ver
     * em real, e quem lia em português não conseguia ver em dólar. São duas
     * escolhas diferentes — tem brasileiro que acompanha o mercado em dólar.
     */
    fun moeda(ctx: Context): String = p(ctx).getString(K_MOEDA, AUTO) ?: AUTO

    fun salvarMoeda(ctx: Context, valor: String) {
        val limpo = if (valor == "brl" || valor == "usd") valor else AUTO
        p(ctx).edit().putString(K_MOEDA, limpo).apply()
        Guardados.moedaValendo = if (limpo == AUTO) "" else limpo
    }

    /** Chamado no início do processo, junto com o idioma. */
    fun carregarNoProcesso(ctx: Context) {
        val m = moeda(ctx)
        Guardados.moedaValendo = if (m == AUTO) "" else m
    }

    // --------------------------------------------------------------- widget
    /** Quantas moedas extras aparecem embaixo do Bitcoin no widget (0 a 5). */
    fun widgetQuantas(ctx: Context): Int = p(ctx).getInt(K_WIDGET_QTD, 0).coerceIn(0, 5)

    fun salvarWidgetQuantas(ctx: Context, n: Int) {
        p(ctx).edit().putInt(K_WIDGET_QTD, n.coerceIn(0, 5)).apply()
    }

    fun widgetFaixa(ctx: Context): Boolean = p(ctx).getBoolean(K_WIDGET_FAIXA, true)
    fun salvarWidgetFaixa(ctx: Context, v: Boolean) { p(ctx).edit().putBoolean(K_WIDGET_FAIXA, v).apply() }

    fun widgetVolume(ctx: Context): Boolean = p(ctx).getBoolean(K_WIDGET_VOLUME, true)
    fun salvarWidgetVolume(ctx: Context, v: Boolean) { p(ctx).edit().putBoolean(K_WIDGET_VOLUME, v).apply() }

    // --------------------------------------------------------------- avisos
    fun avisoNoticias(ctx: Context): Boolean = p(ctx).getBoolean(K_AVISO_NOTICIAS, true)
    fun salvarAvisoNoticias(ctx: Context, v: Boolean) { p(ctx).edit().putBoolean(K_AVISO_NOTICIAS, v).apply() }

    fun avisoResumo(ctx: Context): Boolean = p(ctx).getBoolean(K_AVISO_RESUMO, true)
    fun salvarAvisoResumo(ctx: Context, v: Boolean) { p(ctx).edit().putBoolean(K_AVISO_RESUMO, v).apply() }

    /* ---------------------------------------------------------------------
       Janelas de envio.

       Duas por dia, de manhã e à noite. Não existe agendamento por horário
       exato que sobreviva ao Android moderno sem alarme exato (que precisa de
       permissão e é para despertador, não para isto). Então o verificador, que
       já roda de quinze em quinze minutos, pergunta a cada volta: "estou dentro
       da janela e ainda não mandei a desta janela?".
       --------------------------------------------------------------------- */
    private const val MANHA = 9
    private const val NOITE = 20

    /** 0 = fora de janela, 1 = manhã, 2 = noite. */
    fun janelaAgora(): Int {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (h) {
            in MANHA until MANHA + 2 -> 1
            in NOITE until NOITE + 2 -> 2
            else -> 0
        }
    }

    /** Identificador do dia+janela, para não repetir o mesmo aviso. */
    private fun marca(janela: Int): Long {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR) * 10000L + c.get(Calendar.DAY_OF_YEAR) * 10L + janela
    }

    fun podeMandarResumo(ctx: Context, janela: Int): Boolean =
        janela > 0 && p(ctx).getLong(K_ULTIMO_RESUMO, 0L) != marca(janela)

    fun marcarResumoEnviado(ctx: Context, janela: Int) {
        p(ctx).edit().putLong(K_ULTIMO_RESUMO, marca(janela)).apply()
    }

    /** Data da notícia mais recente que já entrou num aviso. */
    fun ultimaNoticiaAvisada(ctx: Context): Long = p(ctx).getLong(K_ULTIMA_NOTICIA_AVISADA, 0L)

    fun marcarNoticiaAvisada(ctx: Context, quando: Long) {
        if (quando > ultimaNoticiaAvisada(ctx)) {
            p(ctx).edit().putLong(K_ULTIMA_NOTICIA_AVISADA, quando).apply()
        }
    }
}
