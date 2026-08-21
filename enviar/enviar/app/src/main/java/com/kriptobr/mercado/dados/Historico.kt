package com.kriptobr.mercado.dados

import android.content.Context

/**
 * Fita curta de preços recentes, só para o alerta de variação brusca.
 *
 * Para responder "caiu mais de 5% na última hora?" é preciso saber quanto valia
 * uma hora atrás — e nenhuma API pública devolve isso de graça a cada quinze
 * minutos. Então o próprio app anota, no aparelho, uma amostra por vez que o
 * verificador roda.
 *
 * Só guarda as moedas que têm alerta de variação criado. Anotar as cem maiores
 * seria encher o disco de quem nem usa o recurso.
 */
object Historico {

    private const val ARQ = "kriptobr_historico"
    private const val JANELA = 26L * 3600 * 1000      // 26 h cobre a janela de 24 h com folga
    private const val PASSO = 9L * 60 * 1000          // uma amostra a cada ~10 min

    private fun p(ctx: Context) = ctx.getSharedPreferences(ARQ, Context.MODE_PRIVATE)

    /* Formato de propósito burro: "t,preço;t,preço". JSON aqui seria três vezes
       maior para guardar dois números. */
    private fun ler(ctx: Context, id: String): List<Pair<Long, Double>> {
        val bruto = p(ctx).getString(id, null) ?: return emptyList()
        return bruto.split(';').mapNotNull { item ->
            val v = item.split(',')
            if (v.size != 2) return@mapNotNull null
            val t = v[0].toLongOrNull() ?: return@mapNotNull null
            val preco = v[1].toDoubleOrNull() ?: return@mapNotNull null
            t to preco
        }
    }

    fun registrar(ctx: Context, moedas: List<Moeda>, interessa: Set<String>) {
        if (interessa.isEmpty()) {
            // último alerta de variação apagado: a fita não serve mais para nada
            if (p(ctx).all.isNotEmpty()) limpar(ctx)
            return
        }
        val agora = System.currentTimeMillis()
        val e = p(ctx).edit()
        var mudou = false
        for (m in moedas) {
            if (m.id !in interessa || m.preco <= 0.0) continue
            val fita = ler(ctx, m.id)
            if (fita.isNotEmpty() && agora - fita.last().first < PASSO) continue
            val nova = (fita + (agora to m.preco)).filter { agora - it.first <= JANELA }
            e.putString(m.id, nova.joinToString(";") { "${it.first},${it.second}" })
            mudou = true
        }
        /* Moeda cujo alerta foi apagado não precisa continuar sendo anotada. */
        p(ctx).all.keys.filterNot { it in interessa }.forEach { e.remove(it); mudou = true }
        if (mudou) e.apply()
    }

    /**
     * Preço mais próximo de [atras] milissegundos atrás.
     *
     * Devolve nulo enquanto a fita não cobrir a janela pedida — melhor não
     * avisar nada do que avisar de uma queda calculada contra um preço de dez
     * minutos atrás quando o usuário pediu uma hora.
     */
    fun precoHa(ctx: Context, id: String, atras: Long): Double? {
        val fita = ler(ctx, id)
        if (fita.isEmpty()) return null
        val alvo = System.currentTimeMillis() - atras
        val maisVelha = fita.first().first
        if (maisVelha > alvo + 10L * 60 * 1000) return null      // ainda não cobre a janela
        return fita.minByOrNull { kotlin.math.abs(it.first - alvo) }?.second
    }

    fun limpar(ctx: Context) {
        p(ctx).edit().clear().apply()
    }
}
