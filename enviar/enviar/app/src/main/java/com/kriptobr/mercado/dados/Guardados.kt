package com.kriptobr.mercado.dados

import android.content.Context
import com.kriptobr.mercado.alerta.Alerta
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Tudo que o app precisa lembrar entre uma abertura e outra: favoritos, alertas
 * e a última cotação recebida. O último preço guardado é o que faz o widget e a
 * tela nunca aparecerem vazios quando a internet falha.
 */
object Guardados {

    private const val ARQ = "kriptobr"
    private const val K_FAV = "favoritos"
    private const val K_ALERTAS = "alertas"
    private const val K_CACHE = "cache_moedas"
    private const val K_CACHE_QUANDO = "cache_quando"
    private const val K_MEDO = "cache_medo"
    private const val K_FONTE = "fonte_precos"

    private fun p(ctx: Context) = ctx.getSharedPreferences(ARQ, Context.MODE_PRIVATE)

    /**
     * Real no Brasil, dólar no resto do mundo. Quando a pessoa escolhe o idioma
     * na mão, a moeda acompanha: português mostra em real, inglês e espanhol em
     * dólar — do mesmo jeito que o painel na web.
     */
    fun fiat(): String {
        // escolha explícita do usuário ganha do idioma
        if (moedaValendo.isNotEmpty()) return moedaValendo
        return when (idiomaValendo) {
            "pt" -> "brl"
            "en", "es" -> "usd"
            else -> if (Locale.getDefault().country.equals("BR", true)) "brl" else "usd"
        }
    }

    /* Preenchido pelo App no início do processo, como o idioma. Vazio = seguir
       o idioma, que é o comportamento antigo. */
    @Volatile
    var moedaValendo: String = ""

    /* Preenchido pelo App no início do processo. Sem contexto aqui, Formato
       precisa continuar podendo chamar fiat() sem receber nada. */
    @Volatile
    var idiomaValendo: String = ""

    // ---------- de onde vem o preço ----------
    fun fonte(ctx: Context): String =
        p(ctx).getString(K_FONTE, Corretoras.MEDIA) ?: Corretoras.MEDIA

    fun salvarFonte(ctx: Context, id: String) {
        p(ctx).edit().putString(K_FONTE, id).apply()
    }

    // ---------- favoritos ----------
    fun favoritos(ctx: Context): List<String> {
        val bruto = p(ctx).getString(K_FAV, null) ?: return Api.PADRAO
        val arr = JSONArray(bruto)
        val lista = (0 until arr.length()).map { arr.getString(it) }
        return lista.ifEmpty { Api.PADRAO }
    }

    fun salvarFavoritos(ctx: Context, ids: List<String>) {
        p(ctx).edit().putString(K_FAV, JSONArray(ids).toString()).apply()
    }

    fun alternarFavorito(ctx: Context, id: String): List<String> {
        val atual = favoritos(ctx).toMutableList()
        if (!atual.remove(id)) atual.add(id)
        val fixo = atual.ifEmpty { listOf("bitcoin") }
        salvarFavoritos(ctx, fixo)
        return fixo
    }

    // ---------- alertas ----------
    fun alertas(ctx: Context): List<Alerta> {
        val bruto = p(ctx).getString(K_ALERTAS, null) ?: return emptyList()
        val arr = JSONArray(bruto)
        return (0 until arr.length()).mapNotNull { i ->
            runCatching { Alerta.deJson(arr.getJSONObject(i)) }.getOrNull()
        }
    }

    fun salvarAlertas(ctx: Context, lista: List<Alerta>) {
        val arr = JSONArray()
        lista.forEach { arr.put(it.paraJson()) }
        p(ctx).edit().putString(K_ALERTAS, arr.toString()).apply()
    }

    // ---------- último mercado recebido ----------
    fun salvarMercado(ctx: Context, m: Mercado) {
        if (m.moedas.isEmpty()) return
        val arr = JSONArray()
        m.moedas.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id); put("simbolo", c.simbolo); put("nome", c.nome)
                put("preco", c.preco); put("var", c.variacao24h); put("cap", c.capMercado)
                put("alta", c.maxima24h); put("baixa", c.minima24h); put("vol", c.volume24h)
                put("fonte", c.fonte); put("conv", c.convertido)
                put("hist", JSONArray(c.historico.takeLast(60)))
            })
        }
        val e = p(ctx).edit()
            .putString(K_CACHE, arr.toString())
            .putLong(K_CACHE_QUANDO, System.currentTimeMillis())
        m.medo?.let {
            e.putString(K_MEDO, JSONObject().apply {
                put("v", it.valor); put("o", it.ontem ?: -1); put("s", it.semana ?: -1)
            }.toString())
        }
        e.apply()
    }

    fun mercadoGuardado(ctx: Context): Mercado {
        val bruto = p(ctx).getString(K_CACHE, null) ?: return Mercado()
        val arr = runCatching { JSONArray(bruto) }.getOrNull() ?: return Mercado()
        val moedas = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val h = o.optJSONArray("hist")
            Moeda(
                id = o.getString("id"),
                simbolo = o.getString("simbolo"),
                nome = o.getString("nome"),
                preco = o.getDouble("preco"),
                variacao24h = o.getDouble("var"),
                capMercado = o.optDouble("cap", 0.0),
                maxima24h = o.optDouble("alta", 0.0),
                minima24h = o.optDouble("baixa", 0.0),
                volume24h = o.optDouble("vol", 0.0),
                historico = h?.let { s -> (0 until s.length()).map { s.getDouble(it) } } ?: emptyList(),
                fonte = o.optString("fonte", ""),
                convertido = o.optBoolean("conv", false)
            )
        }
        val medo = p(ctx).getString(K_MEDO, null)?.let {
            runCatching {
                val o = JSONObject(it)
                MedoGanancia(
                    o.getInt("v"),
                    o.optInt("o", -1).takeIf { n -> n >= 0 },
                    o.optInt("s", -1).takeIf { n -> n >= 0 }
                )
            }.getOrNull()
        }
        return Mercado(moedas, medo, p(ctx).getLong(K_CACHE_QUANDO, 0L))
    }
}
