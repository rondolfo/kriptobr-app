package com.kriptobr.mercado.dados

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Uma posição digitada à mão: quanto a pessoa tem e por quanto comprou.
 *
 * Nada de chave de API, nada de conectar corretora, nada de endereço de
 * carteira. O app é de uma revendedora de hardware wallet — pedir a chave da
 * exchange do cliente seria exatamente o contrário do que a KriptoBR vende.
 */
data class Posicao(
    val id: String,
    val simbolo: String,
    val nome: String,
    val quantidade: Double,
    /** Preço médio de compra, na moeda em que foi digitado. */
    val precoMedio: Double,
    val moeda: String
) {
    fun paraJson(): JSONObject = JSONObject().apply {
        put("id", id); put("simbolo", simbolo); put("nome", nome)
        put("qtd", quantidade); put("medio", precoMedio); put("moeda", moeda)
    }

    companion object {
        fun deJson(o: JSONObject) = Posicao(
            id = o.getString("id"),
            simbolo = o.getString("simbolo"),
            nome = o.optString("nome", o.getString("simbolo")),
            quantidade = o.getDouble("qtd"),
            precoMedio = o.optDouble("medio", 0.0),
            moeda = o.optString("moeda", "brl")
        )
    }
}

/** Resultado do cálculo, já convertido para a moeda que a tela está mostrando. */
data class Resumo(
    val investido: Double,
    val valorHoje: Double,
    val lucro: Double,
    val variacao: Double,
    val semPreco: Int
)

object Carteira {

    private const val ARQ = "kriptobr"
    private const val K = "carteira"

    private fun p(ctx: Context) = ctx.getSharedPreferences(ARQ, Context.MODE_PRIVATE)

    fun tudo(ctx: Context): List<Posicao> {
        val bruto = p(ctx).getString(K, null) ?: return emptyList()
        val arr = runCatching { JSONArray(bruto) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            runCatching { Posicao.deJson(arr.getJSONObject(i)) }.getOrNull()
        }
    }

    fun salvar(ctx: Context, lista: List<Posicao>) {
        val arr = JSONArray()
        lista.forEach { arr.put(it.paraJson()) }
        p(ctx).edit().putString(K, arr.toString()).apply()
    }

    /**
     * @param dolar quanto vale um dólar em real; só é usado quando a posição foi
     *              digitada numa moeda diferente da que a tela está mostrando.
     */
    fun calcular(posicoes: List<Posicao>, mercado: Mercado, fiat: String, dolar: Double): Resumo {
        var investido = 0.0
        var hoje = 0.0
        var semPreco = 0
        for (pos in posicoes) {
            val moeda = mercado.acharPor(pos.id)
            if (moeda == null || moeda.preco <= 0.0) { semPreco++; continue }
            val fator = when {
                pos.moeda.equals(fiat, true) -> 1.0
                dolar <= 0.0 -> { semPreco++; continue }
                pos.moeda.equals("usd", true) -> dolar          // digitado em dólar, tela em real
                else -> 1.0 / dolar                              // digitado em real, tela em dólar
            }
            investido += pos.quantidade * pos.precoMedio * fator
            hoje += pos.quantidade * moeda.preco
        }
        val lucro = hoje - investido
        val variacao = if (investido > 0.0) lucro / investido * 100.0 else 0.0
        return Resumo(investido, hoje, lucro, variacao, semPreco)
    }
}
