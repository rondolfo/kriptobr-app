package com.kriptobr.mercado.dados

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Carregador de miniaturas enxuto: memória, disco e rede, nessa ordem.
 *
 * O portal recusa a imagem quando o pedido chega com o cabeçalho Referer e
 * devolve um selo quadrado de 200x200 com status 200 — ou seja, sem erro para
 * capturar. Aqui não mandamos Referer nenhum, mas a checagem do quadrado fica
 * como rede de segurança: se a foto vier quadradinha e pequena, é o selo, e a
 * tela mostra o bloco da marca em vez de uma imagem errada.
 */
object Miniaturas {

    private const val PASTA = "mini"
    private const val VALIDADE = 7L * 24 * 3600 * 1000

    private val memoria = object : LruCache<String, Bitmap>(6 * 1024 * 1024) {
        override fun sizeOf(chave: String, valor: Bitmap): Int = valor.byteCount
    }

    private fun apelido(url: String): String {
        val d = MessageDigest.getInstance("SHA-1").digest(url.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    private fun decodificar(dados: ByteArray, ladoPx: Int): Bitmap? {
        val medida = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(dados, 0, dados.size, medida)
        val maior = maxOf(medida.outWidth, medida.outHeight)
        if (maior <= 0) return null
        var passo = 1
        while (maior / passo > ladoPx * 2) passo *= 2
        val opcoes = BitmapFactory.Options().apply { inSampleSize = passo }
        return BitmapFactory.decodeByteArray(dados, 0, dados.size, opcoes)
    }

    /** O selo de bloqueio do portal: quadrado e pequeno. Foto de notícia não é assim. */
    private fun pareceSelo(b: Bitmap): Boolean =
        b.width == b.height && b.width in 1..220

    suspend fun carregar(ctx: Context, url: String, ladoPx: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            memoria.get(url)?.let { return@withContext it }
            val pasta = File(ctx.cacheDir, PASTA).apply { if (!exists()) mkdirs() }
            val arquivo = File(pasta, apelido(url))

            if (arquivo.exists() && System.currentTimeMillis() - arquivo.lastModified() < VALIDADE) {
                val b = runCatching { decodificar(arquivo.readBytes(), ladoPx) }.getOrNull()
                if (b != null) { memoria.put(url, b); return@withContext b }
            }

            val dados = runCatching { Rede.bytes(url) }.getOrNull() ?: return@withContext null
            val b = runCatching { decodificar(dados, ladoPx) }.getOrNull() ?: return@withContext null
            if (pareceSelo(b)) return@withContext null
            runCatching { arquivo.writeBytes(dados) }
            memoria.put(url, b)
            b
        }

    /** Chamado quando o cache passa dos 12 MB — o disco do usuário não é nosso. */
    suspend fun limparAntigas(ctx: Context) {
        withContext(Dispatchers.IO) {
            runCatching {
                val pasta = File(ctx.cacheDir, PASTA)
                val arquivos = pasta.listFiles() ?: return@runCatching
                if (arquivos.sumOf { it.length() } < 12L * 1024 * 1024) return@runCatching
                arquivos.sortedBy { it.lastModified() }.take(arquivos.size / 2).forEach { it.delete() }
            }
        }
    }
}
