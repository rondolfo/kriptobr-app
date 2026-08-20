package com.kriptobr.mercado.dados

import java.text.NumberFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import kotlin.math.abs

/** Formatação de dinheiro, porcentagem e hora, sempre no idioma do aparelho. */
object Formato {

    private fun local(): Locale = Locale.getDefault()

    fun dinheiro(v: Double, fiat: String = Guardados.fiat()): String {
        val f = NumberFormat.getCurrencyInstance(local())
        runCatching { f.currency = Currency.getInstance(fiat.uppercase()) }
        f.maximumFractionDigits = when {
            abs(v) >= 1000 -> 0
            abs(v) >= 1 -> 2
            else -> 6
        }
        f.minimumFractionDigits = if (abs(v) >= 1000) 0 else 2
        return f.format(v)
    }

    fun dinheiroCheio(v: Double, fiat: String = Guardados.fiat()): String {
        val f = NumberFormat.getCurrencyInstance(local())
        runCatching { f.currency = Currency.getInstance(fiat.uppercase()) }
        f.minimumFractionDigits = 2
        f.maximumFractionDigits = if (abs(v) >= 1) 2 else 6
        return f.format(v)
    }

    fun compacto(v: Double, fiat: String = Guardados.fiat()): String {
        val sinal = if (fiat.equals("brl", true)) "R$" else "US$"
        val (n, s) = when {
            abs(v) >= 1e12 -> v / 1e12 to " tri"
            abs(v) >= 1e9 -> v / 1e9 to " bi"
            abs(v) >= 1e6 -> v / 1e6 to " mi"
            else -> v to ""
        }
        val f = NumberFormat.getNumberInstance(local()).apply { maximumFractionDigits = if (s.isEmpty()) 0 else 2 }
        return "$sinal ${f.format(n)}$s"
    }

    fun porcento(v: Double, comSeta: Boolean = true): String {
        val f = NumberFormat.getNumberInstance(local()).apply {
            minimumFractionDigits = 2; maximumFractionDigits = 2
        }
        val seta = if (!comSeta) "" else if (v >= 0) "▲ " else "▼ "
        return seta + f.format(abs(v)) + "%"
    }

    fun numero(v: Double, casas: Int = 0): String {
        val f = NumberFormat.getNumberInstance(local()).apply { maximumFractionDigits = casas }
        return f.format(v)
    }

    fun hora(quando: Long): String =
        if (quando <= 0) "—" else SimpleDateFormat("HH:mm", local()).format(Date(quando))
}
