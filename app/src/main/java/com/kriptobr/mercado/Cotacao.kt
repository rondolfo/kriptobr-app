package com.kriptobr.mercado

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

/** Uma moeda como o widget precisa exibir. */
data class Moeda(val simbolo: String, val preco: Double, val variacao24h: Double)

/**
 * Busca a cotação direto do CoinGecko, do próprio aparelho — sem servidor no meio.
 * Cada celular é um IP diferente, então o limite gratuito da API não é um problema.
 */
object Cotacao {

    private const val URL_BASE =
        "https://api.coingecko.com/api/v3/coins/markets?vs_currency=%s&ids=bitcoin,ethereum" +
            "&order=market_cap_desc&sparkline=false&price_change_percentage=24h"

    fun buscar(moedaFiat: String): List<Moeda> {
        val con = (URL(URL_BASE.format(moedaFiat)).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "KriptoBRWidget/1.0 (+https://mercado.kriptobr.com)")
        }
        try {
            if (con.responseCode !in 200..299) error("HTTP ${con.responseCode}")
            val texto = con.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(texto)
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Moeda(
                    simbolo = o.getString("symbol").uppercase(Locale.ROOT),
                    preco = o.getDouble("current_price"),
                    variacao24h = o.optDouble("price_change_percentage_24h", 0.0)
                )
            }
        } finally {
            con.disconnect()
        }
    }

    fun formatarPreco(v: Double, moedaFiat: String, local: Locale): String {
        val f = NumberFormat.getCurrencyInstance(local)
        f.currency = java.util.Currency.getInstance(moedaFiat.uppercase(Locale.ROOT))
        f.maximumFractionDigits = if (v >= 1000) 0 else 2
        return f.format(v)
    }

    fun formatarVariacao(v: Double, local: Locale): String {
        val n = NumberFormat.getNumberInstance(local).apply {
            minimumFractionDigits = 2; maximumFractionDigits = 2
        }
        return (if (v >= 0) "▲ " else "▼ ") + n.format(Math.abs(v)) + "%"
    }
}
