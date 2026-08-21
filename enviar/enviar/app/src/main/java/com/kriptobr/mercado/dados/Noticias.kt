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
    private const val MIDIA = "https://kriptohoje.com/wp-json/wp/v2/media"
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

    /*
     * Tamanhos, em ordem de preferência.
     *
     * "thumbnail" ficou de fora de propósito: no WordPress ele é 150x150
     * **cortado em quadrado**, e a proteção de hotlink do portal responde com um
     * selo que também é um quadrado pequeno. Era impossível distinguir os dois,
     * e o app acabava jogando fora toda foto legítima — motivo pelo qual a lista
     * de notícias aparecia sem imagem nenhuma. "medium" tem 300 px de largura e
     * mantém a proporção, então nunca é confundido com o selo.
     */
    private val TAMANHOS = listOf("medium", "td_300x0", "td_218x150", "medium_large", "large")

    private fun melhorTamanho(midia: JSONObject): String? {
        val tamanhos = midia.optJSONObject("media_details")?.optJSONObject("sizes")
        for (nome in TAMANHOS) {
            val u = tamanhos?.optJSONObject(nome)?.optString("source_url")
            if (!u.isNullOrBlank()) return u
        }
        return midia.optString("source_url").takeIf { it.isNotBlank() }
    }

    private fun miniaturaEmbutida(post: JSONObject): String? {
        val mid = post.optJSONObject("_embedded")
            ?.optJSONArray("wp:featuredmedia")
            ?.optJSONObject(0) ?: return null
        return melhorTamanho(mid)
    }

    /*
     * Rede de segurança: o portal tem uma camada de cache que às vezes ignora o
     * ?_embed, e aí o post volta sem a mídia embutida — só com o número dela.
     * Uma consulta só, para todas as imagens que faltaram, resolve.
     */
    private fun buscarMidias(ids: List<Int>): Map<Int, String> {
        if (ids.isEmpty()) return emptyMap()
        val endereco = "$MIDIA?include=${ids.joinToString(",")}&per_page=${ids.size}" +
            "&_fields=id,source_url,media_details"
        return runCatching {
            val arr = JSONArray(Rede.texto(endereco, 15))
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = o.optInt("id", 0)
                val url = melhorTamanho(o)
                if (id > 0 && url != null) id to url else null
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    suspend fun ultimas(quantas: Int = 20): List<Noticia> = withContext(Dispatchers.IO) {
        val endereco = "$API?per_page=$quantas&_embed=wp:featuredmedia" +
            "&_fields=title,link,date_gmt,featured_media,_links,_embedded"
        val arr = JSONArray(Rede.texto(endereco, 20))

        val cruas = (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                val titulo = limpar(o.getJSONObject("title").getString("rendered"))
                val link = o.getString("link")
                if (titulo.isBlank() || link.isBlank()) return@runCatching null
                Triple(
                    Noticia(titulo, link, emMilissegundos(o.optString("date_gmt")), miniaturaEmbutida(o)),
                    o.optInt("featured_media", 0),
                    0
                )
            }.getOrNull()
        }

        val faltam = cruas.filter { it.first.miniatura == null && it.second > 0 }
            .map { it.second }.distinct().take(20)
        if (faltam.isEmpty()) return@withContext cruas.map { it.first }

        val mapa = buscarMidias(faltam)
        cruas.map { (n, midia, _) ->
            if (n.miniatura != null) n else n.copy(miniatura = mapa[midia])
        }
    }
}
