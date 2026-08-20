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
                val moedas = Api.moedas(ids, fiat)
                if (moedas.isEmpty()) error("lista vazia")
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
}
