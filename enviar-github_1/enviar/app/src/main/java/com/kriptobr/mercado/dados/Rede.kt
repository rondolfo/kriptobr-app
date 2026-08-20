package com.kriptobr.mercado.dados

import java.net.HttpURLConnection
import java.net.URL

/**
 * Busca simples de texto e bytes. Sem biblioteca de terceiros: o app faz poucas
 * chamadas e cada dependência a mais é peso no APK e uma versão a mais para
 * acompanhar.
 */
object Rede {

    private const val AGENTE = "KriptoBRApp/2.0 (+https://mercado.kriptobr.com)"

    private fun abrir(endereco: String, segundos: Int, aceita: String): HttpURLConnection =
        (URL(endereco).openConnection() as HttpURLConnection).apply {
            connectTimeout = segundos * 1000
            readTimeout = segundos * 1000
            instanceFollowRedirects = true
            setRequestProperty("Accept", aceita)
            setRequestProperty("User-Agent", AGENTE)
        }

    fun texto(endereco: String, segundos: Int = 20): String {
        val con = abrir(endereco, segundos, "application/json")
        try {
            if (con.responseCode !in 200..299) error("HTTP ${con.responseCode}")
            return con.inputStream.bufferedReader().use { it.readText() }
        } finally {
            con.disconnect()
        }
    }

    /** Limite de tamanho para a imagem não estourar a memória se vier algo enorme. */
    fun bytes(endereco: String, segundos: Int = 15, maximo: Int = 2 * 1024 * 1024): ByteArray? {
        val con = abrir(endereco, segundos, "image/*")
        try {
            if (con.responseCode !in 200..299) return null
            val saida = java.io.ByteArrayOutputStream()
            val pedaco = ByteArray(16 * 1024)
            con.inputStream.use { entrada ->
                while (true) {
                    val lidos = entrada.read(pedaco)
                    if (lidos <= 0) break
                    saida.write(pedaco, 0, lidos)
                    if (saida.size() > maximo) return null
                }
            }
            return saida.toByteArray()
        } catch (e: Exception) {
            return null
        } finally {
            con.disconnect()
        }
    }
}
