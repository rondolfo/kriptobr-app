package com.kriptobr.mercado.dados

import android.content.Context
import kotlinx.coroutines.delay

/**
 * Ponto único de busca. Sempre devolve alguma coisa: se a rede falhar, entrega o
 * último mercado guardado. Quem chama nunca precisa lidar com tela vazia.
 *
 * Tenta três vezes antes de desistir. A primeira chamada logo depois de abrir o
 * app é a que mais falha — o rádio do celular ainda está acordando, e a API
 * pública às vezes responde 429 quando muita gente pede ao mesmo tempo. Uma
 * segunda tentativa três segundos depois quase sempre resolve, e sai muito mais
 * barato do que mostrar "sem conexão" para quem tem conexão.
 */
object Repo {

    private const val TENTATIVAS = 3

    suspend fun carregar(ctx: Context): Result<Mercado> {
        val fiat = Guardados.fiat()
        val ids = (Guardados.favoritos(ctx) + Api.PADRAO).distinct()
        var ultimoErro: Throwable? = null

        for (n in 0 until TENTATIVAS) {
            if (n > 0) delay(if (n == 1) 2000L else 5000L)   // 2 s e depois 5 s
            val r = runCatching {
                val base = Api.moedas(ids, fiat)   // sem gráfico quando a lista é longa
                if (base.isEmpty()) error("lista vazia")
                val moedas = aplicarCorretora(ctx, base, fiat)
                val medo = Api.medoGanancia()
                val m = Mercado(moedas, medo ?: Guardados.mercadoGuardado(ctx).medo, System.currentTimeMillis())
                Guardados.salvarMercado(ctx, m)
                m
            }
            if (r.isSuccess) return r
            ultimoErro = r.exceptionOrNull()
        }
        return Result.failure(ultimoErro ?: IllegalStateException("falha ao carregar"))
    }

    fun ultimoConhecido(ctx: Context): Mercado = Guardados.mercadoGuardado(ctx)

    /**
     * Troca preço, máxima, mínima e volume pelos números da corretora escolhida.
     *
     * O resto continua vindo da média do mercado: nome, ícone, valor de mercado
     * e o gráfico de sete dias, que nenhuma corretora entrega junto. Moeda que a
     * corretora não lista fica como estava — é melhor mostrar o preço médio do
     * que abrir um buraco na lista.
     */
    private suspend fun aplicarCorretora(ctx: Context, base: List<Moeda>, fiat: String): List<Moeda> {
        val fonte = Guardados.fonte(ctx)
        if (fonte == Corretoras.MEDIA) return base

        val alvo = if (fiat.equals("brl", true)) "BRL" else "USD"
        /* Quando a tela está em real, o preço do tether já veio na lista e serve
           de câmbio — uma requisição a menos. */
        val dolar = if (alvo == "BRL") {
            base.firstOrNull { it.id == "tether" }?.preco?.takeIf { it > 0.5 } ?: Api.dolarEmReal()
        } else {
            Api.dolarEmReal()
        }

        val cotacoes = runCatching {
            Corretoras.buscar(fonte, base.map { it.simbolo }, alvo, dolar)
        }.getOrElse { emptyMap() }
        if (cotacoes.isEmpty()) return base

        return base.map { m ->
            val c = cotacoes[m.simbolo] ?: return@map m
            m.copy(
                preco = c.preco,
                variacao24h = c.variacao ?: m.variacao24h,
                maxima24h = c.alta ?: 0.0,
                minima24h = c.baixa ?: 0.0,
                volume24h = c.volume ?: 0.0,
                venda = c.venda ?: 0.0,
                compra = c.compra ?: 0.0,
                fonte = fonte,
                convertido = c.convertido
            )
        }
    }
}
