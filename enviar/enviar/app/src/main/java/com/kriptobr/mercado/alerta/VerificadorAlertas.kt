package com.kriptobr.mercado.alerta

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kriptobr.mercado.Avisos
import com.kriptobr.mercado.R
import com.kriptobr.mercado.dados.Formato
import com.kriptobr.mercado.dados.Guardados
import com.kriptobr.mercado.dados.Repo
import com.kriptobr.mercado.widget.WidgetCotacao
import java.util.concurrent.TimeUnit

/**
 * Roda a cada 15 minutos, mesmo com o app fechado. Busca os preços, avisa os
 * alertas que bateram e, de quebra, atualiza o widget com o dado fresco.
 *
 * É o CoroutineWorker que faz a rede — não o widget. O Android dá só 10 segundos
 * para um widget terminar o que começou, e era exatamente aí que aparecia o
 * "sem conexão" na versão anterior.
 */
class VerificadorAlertas(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val mercado = Repo.carregar(ctx).getOrNull() ?: return Result.retry()

        val alertas = Guardados.alertas(ctx)
        if (alertas.isNotEmpty()) {
            val atualizados = alertas.map { a ->
                if (!a.ativo || a.disparadoEm > 0) return@map a
                val preco = mercado.acharPor(a.moedaId)?.preco ?: return@map a
                val bateu = if (a.acima) preco >= a.alvo else preco <= a.alvo
                if (!bateu) return@map a

                Avisos.mostrar(
                    ctx, Avisos.CANAL_ALERTAS,
                    ctx.getString(
                        if (a.acima) R.string.aviso_subiu else R.string.aviso_caiu,
                        a.nome, Formato.dinheiro(a.alvo)
                    ),
                    ctx.getString(R.string.aviso_corpo, a.simbolo, Formato.dinheiro(preco)),
                    "https://mercado.kriptobr.com/?utm_source=app&utm_medium=alerta",
                    a.id.toInt()
                )
                a.copy(ativo = false, disparadoEm = System.currentTimeMillis())
            }
            if (atualizados != alertas) Guardados.salvarAlertas(ctx, atualizados)
        }

        WidgetCotacao.redesenharTodos(ctx)
        return Result.success()
    }

    companion object {
        private const val NOME = "kbr-alertas"

        fun agendar(ctx: Context) {
            val pedido = PeriodicWorkRequestBuilder<VerificadorAlertas>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                NOME, ExistingPeriodicWorkPolicy.KEEP, pedido
            )
        }
    }
}
