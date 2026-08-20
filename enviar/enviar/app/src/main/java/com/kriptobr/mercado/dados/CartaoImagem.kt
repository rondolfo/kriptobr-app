package com.kriptobr.mercado.dados

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.kriptobr.mercado.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Monta a imagem da cotação para compartilhar.
 *
 * Três formatos porque cada lugar tem o seu: o Stories corta tudo que não for
 * 9:16, o feed do Instagram gosta do quadrado e o WhatsApp e o X ficam melhores
 * deitados. Desenhado no Canvas do próprio Android — nada de fotografar a tela,
 * que sairia com a barra de status e no tamanho do aparelho de cada um.
 */
object CartaoImagem {

    enum class Tamanho(val larg: Int, val alt: Int) {
        STORY(1080, 1920),
        POST(1080, 1080),
        DEITADO(1920, 1080)
    }

    private const val FUNDO = 0xFF091417.toInt()
    private const val SUPERFICIE = 0xFF0F1E23.toInt()
    private const val BORDA = 0xFF26424C.toInt()
    private const val TINTA = 0xFFEAF2F3.toInt()
    private const val APAGADO = 0xFF7A9299.toInt()
    private const val MINT = 0xFF00C69F.toInt()
    private const val ALTA = 0xFF2BD07E.toInt()
    private const val BAIXA = 0xFFF2707B.toInt()
    private const val LARANJA = 0xFFF7931A.toInt()

    private fun tinta(tamanho: Float, cor: Int, negrito: Boolean = false, direita: Boolean = false) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cor
            textSize = tamanho
            typeface = Typeface.create(Typeface.SANS_SERIF, if (negrito) Typeface.BOLD else Typeface.NORMAL)
            textAlign = if (direita) Paint.Align.RIGHT else Paint.Align.LEFT
        }

    suspend fun gerar(ctx: Context, m: Mercado, formato: Tamanho): File? =
        withContext(Dispatchers.IO) {
            runCatching { desenharEGravar(ctx, m, formato) }.getOrNull()
        }

    private fun desenharEGravar(ctx: Context, m: Mercado, f: Tamanho): File {
        val bmp = Bitmap.createBitmap(f.larg, f.alt, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val k = minOf(f.larg, f.alt) / 1080f          // escala: o quadrado é a referência

        // fundo com um brilho suave vindo de cima
        c.drawColor(FUNDO)
        val brilho = Paint().apply {
            shader = LinearGradient(0f, 0f, f.larg.toFloat(), f.alt.toFloat(),
                intArrayOf(0x22007A63, FUNDO, FUNDO), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, f.larg.toFloat(), f.alt.toFloat(), brilho)

        val margem = 76f * k
        val larguraUtil = f.larg - margem * 2

        // ---------- logo ----------
        var y = margem + 40f * k
        val logo = runCatching {
            BitmapFactory.decodeResource(ctx.resources, R.drawable.logo_kriptobr)
        }.getOrNull()
        if (logo != null) {
            val alturaLogo = 58f * k
            val larguraLogo = alturaLogo * logo.width / logo.height
            c.drawBitmap(logo, null, RectF(margem, y, margem + larguraLogo, y + alturaLogo), null)
            y += alturaLogo + 52f * k
        }

        val btc = m.acharPor("bitcoin")

        // ---------- destaque do Bitcoin ----------
        if (btc != null) {
            c.drawText("BITCOIN", margem, y, tinta(30f * k, APAGADO, true).apply { letterSpacing = 0.22f })
            y += 70f * k

            val tamanhoPreco = if (f == Tamanho.DEITADO) 122f * k else 132f * k
            val precoTxt = Formato.dinheiro(btc.preco)
            val pintaPreco = tinta(tamanhoPreco, TINTA, true)
            // encolhe se o número não couber (dólar de seis dígitos em real, por exemplo)
            var t = tamanhoPreco
            while (pintaPreco.measureText(precoTxt) > larguraUtil && t > 40f * k) {
                t -= 4f * k; pintaPreco.textSize = t
            }
            c.drawText(precoTxt, margem, y + t * 0.78f, pintaPreco)
            y += t * 0.78f + 44f * k

            // etiqueta de variação
            val alta = btc.variacao24h >= 0
            val etiqueta = (if (alta) "▲ " else "▼ ") + Formato.porcento(btc.variacao24h).removePrefix("+")
            val pintaEt = tinta(38f * k, if (alta) ALTA else BAIXA, true)
            val largEt = pintaEt.measureText(etiqueta) + 44f * k
            val caixa = RectF(margem, y, margem + largEt, y + 68f * k)
            c.drawRoundRect(caixa, 34f * k, 34f * k,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (alta) ALTA else BAIXA; alpha = 38 })
            c.drawText(etiqueta, margem + 22f * k, caixa.centerY() + 13f * k, pintaEt)
            c.drawText("em 24 h", caixa.right + 22f * k, caixa.centerY() + 11f * k, tinta(30f * k, APAGADO))
            y += 68f * k + 56f * k
        }

        // ---------- outras moedas ----------
        val outras = m.moedas.filter { it.id != "bitcoin" }.take(if (f == Tamanho.POST) 3 else 5)
        outras.forEach { moeda ->
            val alturaLinha = 92f * k
            val cx = RectF(margem, y, margem + larguraUtil, y + alturaLinha)
            c.drawRoundRect(cx, 24f * k, 24f * k, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SUPERFICIE })
            c.drawRoundRect(cx, 24f * k, 24f * k, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = BORDA; style = Paint.Style.STROKE; strokeWidth = 2f * k
            })
            c.drawText(moeda.simbolo, margem + 30f * k, y + alturaLinha * 0.62f, tinta(36f * k, TINTA, true))
            c.drawText(Formato.dinheiro(moeda.preco), margem + larguraUtil - 200f * k,
                y + alturaLinha * 0.62f, tinta(34f * k, TINTA, true, direita = true).apply { textAlign = Paint.Align.RIGHT })
            val a = moeda.variacao24h >= 0
            c.drawText(Formato.porcento(moeda.variacao24h), margem + larguraUtil - 30f * k,
                y + alturaLinha * 0.62f, tinta(32f * k, if (a) ALTA else BAIXA, true).apply { textAlign = Paint.Align.RIGHT })
            y += alturaLinha + 16f * k
        }

        // ---------- termômetro ----------
        m.medo?.let { fg ->
            y += 26f * k
            c.drawText("MEDO E GANÂNCIA", margem, y, tinta(26f * k, APAGADO, true).apply { letterSpacing = 0.2f })
            y += 56f * k
            c.drawText(fg.valor.toString(), margem, y, tinta(64f * k, MINT, true))
            y += 24f * k
        }

        // ---------- rodapé ----------
        val relogio = SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR")).format(Date(
            if (m.atualizadoEm > 0) m.atualizadoEm else System.currentTimeMillis()))
        val baseRodape = f.alt - margem
        c.drawText("mercado.kriptobr.com", margem, baseRodape, tinta(34f * k, MINT, true))
        c.drawText(relogio, margem, baseRodape - 46f * k, tinta(28f * k, APAGADO))
        c.drawRect(margem, baseRodape - 96f * k, margem + 110f * k, baseRodape - 92f * k,
            Paint().apply { color = LARANJA })

        val pasta = File(ctx.cacheDir, "compartilhar").apply { if (!exists()) mkdirs() }
        pasta.listFiles()?.forEach { it.delete() }          // só a mais recente fica
        val arquivo = File(pasta, "kriptobr-${f.name.lowercase()}.png")
        FileOutputStream(arquivo).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return arquivo
    }
}
