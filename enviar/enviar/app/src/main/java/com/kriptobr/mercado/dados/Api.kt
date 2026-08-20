package com.kriptobr.mercado.dados

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Conversa direto com as APIs públicas, do próprio aparelho — sem servidor nosso no meio.
 * Cada celular é um IP diferente, então o limite gratuito não atrapalha.
 */
object Api {

    /** Moedas que sempre vêm, mesmo que o usuário não tenha favoritado nenhuma. */
    val PADRAO = listOf("bitcoin", "ethereum", "solana", "tether", "ripple", "dogecoin")

    private const val COINGECKO = "https://api.coingecko.com/api/v3"
    private const val ALTERNATIVE = "https://api.alternative.me/fng/?limit=31"

    private fun buscarTexto(endereco: String, segundos: Int = 20): String {
        val con = (URL(endereco).openConnection() as HttpURLConnection).apply {
            connectTimeout = segundos * 1000
            readTimeout = segundos * 1000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "KriptoBRApp/2.0 (+https://mercado.kriptobr.com)")
        }
        try {
            if (con.responseCode !in 200..299) error("HTTP ${con.responseCode}")
            return con.inputStream.bufferedReader().use { it.readText() }
        } finally {
            con.disconnect()
        }
    }

    suspend fun moedas(ids: List<String>, fiat: String): List<Moeda> = withContext(Dispatchers.IO) {
        val lista = ids.ifEmpty { PADRAO }
        /* O gráfico de 7 dias custa ~35 KB por moeda. Com uma lista curta vale a
           pena; com trinta moedas seria mais de um mega e a busca começaria a
           estourar o tempo em rede fraca. Passando disso, vão só os preços. */
        val comGrafico = lista.size <= 25
        val endereco = "$COINGECKO/coins/markets?vs_currency=$fiat" +
            "&ids=${lista.joinToString(",")}" +
            "&order=market_cap_desc&sparkline=$comGrafico&price_change_percentage=24h"
        // com sparkline a resposta passa de 200 KB; em rede fraca 20 s não bastam
        val arr = JSONArray(buscarTexto(endereco, 30))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val spark = o.optJSONObject("sparkline_in_7d")?.optJSONArray("price")
            Moeda(
                id = o.getString("id"),
                simbolo = o.getString("symbol").uppercase(),
                nome = o.getString("name"),
                preco = o.getDouble("current_price"),
                variacao24h = o.optDouble("price_change_percentage_24h", 0.0),
                capMercado = o.optDouble("market_cap", 0.0),
                historico = spark?.let { s -> (0 until s.length()).map { s.getDouble(it) } } ?: emptyList()
            )
        }
    }

    suspend fun medoGanancia(): MedoGanancia? = withContext(Dispatchers.IO) {
        runCatching {
            val d = JSONObject(buscarTexto(ALTERNATIVE, 15)).getJSONArray("data")
            fun em(i: Int): Int? = if (d.length() > i) d.getJSONObject(i).getString("value").toIntOrNull() else null
            MedoGanancia(em(0) ?: return@runCatching null, em(1), em(7))
        }.getOrNull()
    }

    /** Lista para o usuário escolher favoritos. Sem gráfico, então 250 cabem numa consulta só. */
    suspend fun catalogo(fiat: String): List<Moeda> = withContext(Dispatchers.IO) {
        val endereco = "$COINGECKO/coins/markets?vs_currency=$fiat" +
            "&order=market_cap_desc&per_page=250&page=1&sparkline=false&price_change_percentage=24h"
        val arr = JSONArray(buscarTexto(endereco))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Moeda(
                id = o.getString("id"),
                simbolo = o.getString("symbol").uppercase(),
                nome = o.getString("name"),
                preco = o.getDouble("current_price"),
                variacao24h = o.optDouble("price_change_percentage_24h", 0.0),
                capMercado = o.optDouble("market_cap", 0.0)
            )
        }
    }
}
