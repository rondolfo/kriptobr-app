package com.kriptobr.mercado.dados

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * De onde vem o preço.
 *
 * O padrão é a média do mercado (CoinGecko), que é o número "oficial" que todo
 * site mostra. Mas quem opera de verdade quer o preço da corretora onde ele
 * compra — e no Brasil a diferença não é detalhe: o ágio local costuma passar
 * de 1%. Então dá para fixar uma corretora e ver o preço, a máxima, a mínima e
 * o volume dela.
 *
 * Cada corretora devolve o mesmo formato. O volume vai sempre em dinheiro, não
 * em quantidade de moeda — "R$ 19 milhões" diz alguma coisa, "50,95 BTC" não.
 */
object Corretoras {

    const val MEDIA = "media"

    data class Fonte(val id: String, val nome: String, val selo: String)

    /** As oito opções que aparecem na tela de ajustes. */
    val LISTA = listOf(
        Fonte(MEDIA, "Média do mercado", ""),
        Fonte("mb", "Mercado Bitcoin", "BR"),
        Fonte("foxbit", "Foxbit", "BR"),
        Fonte("binance", "Binance", ""),
        Fonte("coinbase", "Coinbase", ""),
        Fonte("kraken", "Kraken", ""),
        Fonte("okx", "OKX", ""),
        Fonte("bitstamp", "Bitstamp", "")
    )

    fun nomeDe(id: String): String = LISTA.firstOrNull { it.id == id }?.nome ?: LISTA[0].nome

    /** Uma cotação já convertida para a moeda que a tela está mostrando. */
    data class Cotacao(
        val preco: Double,
        val alta: Double?,
        val baixa: Double?,
        val volume: Double?,
        val variacao: Double?,
        val convertido: Boolean
    )

    /* Cotação crua, ainda na moeda da corretora. */
    private data class Crua(
        val preco: Double, val alta: Double?, val baixa: Double?,
        val volume: Double?, val variacao: Double?, val em: String
    )

    private const val AO_MESMO_TEMPO = 6

    private fun texto(endereco: String, segundos: Int = 12): String {
        val con = (URL(endereco).openConnection() as HttpURLConnection).apply {
            connectTimeout = segundos * 1000
            readTimeout = segundos * 1000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "KriptoBRApp/2.1 (+https://mercado.kriptobr.com)")
        }
        try {
            if (con.responseCode !in 200..299) error("HTTP ${con.responseCode}")
            return con.inputStream.bufferedReader().use { it.readText() }
        } finally {
            con.disconnect()
        }
    }

    private fun objeto(endereco: String) = JSONObject(texto(endereco))
    private fun vetor(endereco: String) = JSONArray(texto(endereco))

    private fun d(o: JSONObject, chave: String): Double? =
        o.optString(chave, "").toDoubleOrNull()

    private fun variacaoPorAbertura(preco: Double, abertura: Double?): Double? =
        if (abertura == null || abertura <= 0.0) null else (preco - abertura) / abertura * 100.0

    /** Roda as buscas por símbolo numa fila curta, para não abrir 25 conexões de uma vez. */
    private suspend fun emFila(
        simbolos: List<String>,
        busca: suspend (String) -> Crua?
    ): Map<String, Crua> = coroutineScope {
        val vaga = Semaphore(AO_MESMO_TEMPO)
        simbolos.map { s ->
            async(Dispatchers.IO) {
                vaga.withPermit { s to runCatching { busca(s) }.getOrNull() }
            }
        }.awaitAll().mapNotNull { (s, c) -> c?.let { s to it } }.toMap()
    }

    // ---------------------------------------------------------------- Binance
    /* data-api.binance.vision é o espelho público de dados de mercado da
       Binance: mesmos números, sem login e sem bloqueio por região. */
    private suspend fun binance(simbolos: List<String>, alvo: String): Map<String, Crua> =
        emFila(simbolos) { s ->
            if (s == "USDT" && alvo == "USD") Crua(1.0, 1.0, 1.0, null, 0.0, "USD")
            else {
                val tenta = if (alvo == "BRL") listOf(s + "BRL", s + "USDT") else listOf(s + "USDT")
                var achou: Crua? = null
                for (par in tenta) {
                    val r = runCatching {
                        val o = objeto("https://data-api.binance.vision/api/v3/ticker/24hr?symbol=$par")
                        val preco = d(o, "lastPrice") ?: error("sem preço")
                        Crua(
                            preco, d(o, "highPrice"), d(o, "lowPrice"),
                            d(o, "quoteVolume"), d(o, "priceChangePercent"),
                            if (par.endsWith("BRL")) "BRL" else "USD"
                        )
                    }.getOrNull()
                    if (r != null) { achou = r; break }
                }
                achou
            }
        }

    // -------------------------------------------------------- Mercado Bitcoin
    private suspend fun mercadoBitcoin(simbolos: List<String>): Map<String, Crua> =
        withContext(Dispatchers.IO) {
            val lista = simbolos.joinToString(",") { "$it-BRL" }
            val arr = vetor("https://api.mercadobitcoin.net/api/v4/tickers?symbols=$lista")
            val saida = HashMap<String, Crua>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val s = o.optString("pair").substringBefore("-").uppercase()
                val preco = d(o, "last") ?: continue
                if (s.isEmpty() || preco <= 0.0) continue
                val volBase = d(o, "vol")
                saida[s] = Crua(
                    preco, d(o, "high"), d(o, "low"),
                    volBase?.let { it * preco },
                    variacaoPorAbertura(preco, d(o, "open")), "BRL"
                )
            }
            saida
        }

    // ----------------------------------------------------------------- Foxbit
    /* A API da Foxbit ignora o filtro de mercados e devolve os ~130 de uma vez.
       Vale guardar por um minuto em vez de pedir de novo a cada moeda. */
    @Volatile private var foxGuardado: JSONArray? = null
    @Volatile private var foxQuando = 0L

    private suspend fun foxbit(simbolos: List<String>): Map<String, Crua> =
        withContext(Dispatchers.IO) {
            val agora = System.currentTimeMillis()
            var dados = foxGuardado
            if (dados == null || agora - foxQuando > 60_000L) {
                dados = objeto("https://api.foxbit.com.br/rest/v3/markets/ticker/24hr").optJSONArray("data")
                foxGuardado = dados
                foxQuando = agora
            }
            val quer = simbolos.associateBy { (it + "BRL").lowercase() }
            val saida = HashMap<String, Crua>()
            val arr = dados ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val s = quer[o.optString("market_symbol").lowercase()] ?: continue
                val r = o.optJSONObject("rolling_24h") ?: continue
                val preco = o.optJSONObject("last_trade")?.let { d(it, "price") } ?: continue
                if (preco <= 0.0) continue
                saida[s] = Crua(
                    preco, d(r, "high"), d(r, "low"),
                    d(r, "quote_volume"), d(r, "price_change_percent"), "BRL"
                )
            }
            saida
        }

    // --------------------------------------------------------------- Coinbase
    private suspend fun coinbase(simbolos: List<String>): Map<String, Crua> =
        emFila(simbolos) { s ->
            val o = objeto("https://api.exchange.coinbase.com/products/$s-USD/stats")
            val preco = d(o, "last") ?: return@emFila null
            if (preco <= 0.0) return@emFila null
            val volBase = d(o, "volume")
            Crua(
                preco, d(o, "high"), d(o, "low"),
                volBase?.let { it * preco },
                variacaoPorAbertura(preco, d(o, "open")), "USD"
            )
        }

    // ----------------------------------------------------------------- Kraken
    /* A Kraken usa nomes próprios: bitcoin é XBT e dogecoin é XDG. */
    private val KRAKEN_NOME = mapOf("BTC" to "XBT", "DOGE" to "XDG")

    private suspend fun kraken(simbolos: List<String>): Map<String, Crua> =
        emFila(simbolos) { s ->
            val par = (KRAKEN_NOME[s] ?: s) + "USD"
            val j = objeto("https://api.kraken.com/0/public/Ticker?pair=$par")
            val res = j.optJSONObject("result") ?: return@emFila null
            val chave = res.keys().asSequence().firstOrNull() ?: return@emFila null
            val o = res.optJSONObject(chave) ?: return@emFila null
            fun item(nome: String, i: Int): Double? =
                o.optJSONArray(nome)?.optString(i)?.toDoubleOrNull()
            val preco = item("c", 0) ?: return@emFila null
            if (preco <= 0.0) return@emFila null
            val volBase = item("v", 1)
            Crua(
                preco, item("h", 1), item("l", 1),
                volBase?.let { it * preco },
                variacaoPorAbertura(preco, d(o, "o")), "USD"
            )
        }

    // -------------------------------------------------------------------- OKX
    private suspend fun okx(simbolos: List<String>): Map<String, Crua> =
        emFila(simbolos) { s ->
            if (s == "USDT") Crua(1.0, 1.0, 1.0, null, 0.0, "USD")
            else {
                val j = objeto("https://www.okx.com/api/v5/market/ticker?instId=$s-USDT")
                val o = j.optJSONArray("data")?.optJSONObject(0)
                val preco = o?.let { d(it, "last") }
                if (o == null || preco == null || preco <= 0.0) null
                else Crua(
                    preco, d(o, "high24h"), d(o, "low24h"),
                    d(o, "volCcy24h"),
                    variacaoPorAbertura(preco, d(o, "open24h")), "USD"
                )
            }
        }

    // --------------------------------------------------------------- Bitstamp
    @Volatile private var bitGuardado: JSONArray? = null
    @Volatile private var bitQuando = 0L

    private suspend fun bitstamp(simbolos: List<String>): Map<String, Crua> =
        withContext(Dispatchers.IO) {
            val agora = System.currentTimeMillis()
            var dados = bitGuardado
            if (dados == null || agora - bitQuando > 60_000L) {
                dados = vetor("https://www.bitstamp.net/api/v2/ticker/")
                bitGuardado = dados
                bitQuando = agora
            }
            val quer = simbolos.associateBy { "$it/USD" }
            val saida = HashMap<String, Crua>()
            val arr = dados ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val s = quer[o.optString("pair").uppercase()] ?: continue
                val preco = d(o, "last") ?: continue
                if (preco <= 0.0) continue
                val volBase = d(o, "volume")
                saida[s] = Crua(
                    preco, d(o, "high"), d(o, "low"),
                    volBase?.let { it * preco },
                    d(o, "percent_change_24"), "USD"
                )
            }
            saida
        }

    /**
     * @param fonte    id da corretora (ver [LISTA])
     * @param simbolos BTC, ETH… em maiúsculas
     * @param alvo     "BRL" ou "USD" — a moeda em que a tela está
     * @param dolar    quanto vale um dólar em real; só serve para converter
     */
    suspend fun buscar(
        fonte: String,
        simbolos: List<String>,
        alvo: String,
        dolar: Double
    ): Map<String, Cotacao> {
        if (fonte == MEDIA || simbolos.isEmpty()) return emptyMap()
        val cruas = runCatching {
            when (fonte) {
                "binance" -> binance(simbolos, alvo)
                "mb" -> mercadoBitcoin(simbolos)
                "foxbit" -> foxbit(simbolos)
                "coinbase" -> coinbase(simbolos)
                "kraken" -> kraken(simbolos)
                "okx" -> okx(simbolos)
                "bitstamp" -> bitstamp(simbolos)
                else -> emptyMap()
            }
        }.getOrElse { emptyMap() }

        val cambio = if (dolar > 0.0) dolar else 0.0
        val saida = HashMap<String, Cotacao>()
        for ((s, c) in cruas) {
            val converte = c.em != alvo
            if (converte && cambio <= 0.0) continue      // sem câmbio não se inventa número
            val fator = if (!converte) 1.0 else if (c.em == "USD") cambio else 1.0 / cambio
            saida[s] = Cotacao(
                preco = c.preco * fator,
                alta = c.alta?.times(fator),
                baixa = c.baixa?.times(fator),
                volume = c.volume?.times(fator),
                variacao = c.variacao,
                convertido = converte
            )
        }
        return saida
    }
}
