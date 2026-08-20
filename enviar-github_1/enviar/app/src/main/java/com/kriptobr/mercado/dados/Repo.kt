package com.kriptobr.mercado.dados

import android.content.Context

/**
 * Ponto único de busca. Sempre devolve alguma coisa: se a rede falhar, entrega o
 * último mercado guardado. Quem chama nunca precisa lidar com tela vazia.
 */
object Repo {

    suspend fun carregar(ctx: Context): Result<Mercado> {
        val fiat = Guardados.fiat()
        val ids = (Guardados.favoritos(ctx) + Api.PADRAO).distinct()
        return runCatching {
            val moedas = Api.moedas(ids, fiat)
            val medo = Api.medoGanancia()
            val m = Mercado(moedas, medo ?: Guardados.mercadoGuardado(ctx).medo, System.currentTimeMillis())
            Guardados.salvarMercado(ctx, m)
            m
        }
    }

    fun ultimoConhecido(ctx: Context): Mercado = Guardados.mercadoGuardado(ctx)
}
