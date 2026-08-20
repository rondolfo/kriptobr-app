package com.kriptobr.mercado.dados

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Uma manchete do portal KriptoHoje. */
data class Noticia(
    val titulo: String,
    val link: String,
    val quando: Long,
    val miniatura: String?
)

/**
 * Manchetes do KriptoHoje pela API pública do WordPress.
 *
 * A consulta pede a miniatura pequena de propósito: vinte fotos grandes seriam
 * meio megabyte no plano de dados de quem só queria ler os títulos.
 *
 * O que é "novo": tudo publicado depois da marca gravada na última vez que a
 * pessoa abriu esta aba. Na primeira vez não há marca — e aí nada é novo,
 * porque destacar vinte itens de uma vez não destaca nada.
 */
object Noticias {

    private const val API = "https://kriptohoje.com/wp-json/wp/v2/posts"
    private const val UTM = "utm_source=app&utm_medium=android&utm_campaign=kriptohoje"
    private const val ARQ = "kriptobr"
    private const val K_VISTO = "noticias_visto"

    private fun p(ctx: Context) = ctx.getSharedPreferences(ARQ, Context.MODE_PRIVATE)

    fun visto(ctx: Context): Long = p(ctx).getLong(K_VISTO, 0L)

    fun marcarVisto(ctx: Context, quando: Long) {
        if (quando > visto(ctx)) p(ctx).edit().putLong(K_VISTO, quando).apply()
    }

    fun comUtm(link: String): String = link + (if (link.contains("?")) "&" else "?") + UTM

    /** O WordPress devolve "2026-08-20T12:34:56", sempre em UTC, sem o fuso escrito. */
    private fun emMilissegundos(bruto: String): Long = runCatching {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        f.parse(bruto.substringBefore('.').removeSuffix("Z"))?.time ?: 0L
    }.getOrDefault(0L)

    /** Tira tags e devolve as entidades do WordPress (&#8217;, &amp;) ao normal. */
    private fun limpar(bruto: String): String {
        var s = bruto.replace(Regex("<[^>]*>"), "")
        s = Regex("&#(\\d+);").replace(s) { m ->
            m.groupValues[1].toIntOrNull()?.let { Char(it).toString() } ?: m.value
        }
        s = Regex("&#x([0-9a-fA-F]+);").replace(s) { m ->
            m.groupValues[1].toIntOrNull(16)?.let { Char(it).toString() } ?: m.value
        }
        return s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#039;", "'").replace("&apos;", "'")
            .replace("&nbsp;", " ").replace("&hellip;", "…").replace("&ndash;", "–")
            .replace("&mdash;", "—").replace("&rsquo;", "’").replace("&lsquo;", "‘")
            .replace("&ldquo;", "“").replace("&rdquo;", "”").trim()
    }

    private fun miniaturaDe(post: JSONObject): String? {
        val mid = post.optJSONObject("_embedded")
            ?.optJSONArray("wp:featuredmedia")
            ?.optJSONObject(0) ?: return null
        val tamanhos = mid.optJSONObject("media_details")?.optJSONObject("sizes")
        for (nome in listOf("thumbnail", "medium", "medium_large")) {
            val u = tamanhos?.optJSONObject(nome)?.optString("source_url")
            if (!u.isNullOrBlank()) return u
        }
        return mid.optString("source_url").takeIf { it.isNotBlank() }
    }

    suspend fun ultimas(quantas: Int = 20): List<Noticia> = withContext(Dispatchers.IO) {
        val endereco = "$API?per_page=$quantas&_embed=wp:featuredmedia" +
            "&_fields=title,link,date_gmt,_links,_embedded"
        val arr = JSONArray(Rede.texto(endereco, 20))
        (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                val titulo = limpar(o.getJSONObject("title").getString("rendered"))
                val link = o.getString("link")
                if (titulo.isBlank() || link.isBlank()) return@runCatching null
                Noticia(
                    titulo = titulo,
                    link = link,
                    quando = emMilissegundos(o.optString("date_gmt")),
                    miniatura = miniaturaDe(o)
                )
            }.getOrNull()
        }
    }
}
