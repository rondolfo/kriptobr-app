package com.kriptobr.mercado.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kriptobr.mercado.MainActivity
import com.kriptobr.mercado.R
import com.kriptobr.mercado.alerta.VerificadorAlertas
import com.kriptobr.mercado.dados.Formato
import com.kriptobr.mercado.dados.Guardados

/**
 * Widget da tela inicial.
 *
 * Regra de ouro daqui: **este arquivo nunca acessa a rede**. Ele só desenha o
 * último preço guardado, que é sempre instantâneo. Quem busca da internet é o
 * WorkManager, que pode demorar o quanto precisar. Foi a mistura das duas coisas
 * que fazia aparecer "sem conexão" na versão anterior.
 */
class WidgetCotacao : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, gerente: AppWidgetManager, ids: IntArray) {
        desenhar(ctx, gerente, ids)
        pedirDadoNovo(ctx)
    }

    override fun onEnabled(ctx: Context) {
        VerificadorAlertas.agendar(ctx)
        pedirDadoNovo(ctx)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACAO_ATUALIZAR) {
            marcarCarregando(ctx)
            pedirDadoNovo(ctx)
        }
    }

    companion object {
        const val ACAO_ATUALIZAR = "com.kriptobr.mercado.ATUALIZAR_WIDGET"

        fun redesenharTodos(ctx: Context) {
            val gerente = AppWidgetManager.getInstance(ctx)
            val ids = gerente.getAppWidgetIds(ComponentName(ctx, WidgetCotacao::class.java))
            if (ids.isNotEmpty()) desenhar(ctx, gerente, ids)
        }

        private fun pedirDadoNovo(ctx: Context) {
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                "kbr-widget-agora",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<VerificadorAlertas>().build()
            )
        }

        private fun marcarCarregando(ctx: Context) {
            val gerente = AppWidgetManager.getInstance(ctx)
            val ids = gerente.getAppWidgetIds(ComponentName(ctx, WidgetCotacao::class.java))
            if (ids.isNotEmpty()) desenhar(ctx, gerente, ids, ctx.getString(R.string.widget_atualizando))
        }

        private fun desenhar(
            ctx: Context,
            gerente: AppWidgetManager,
            ids: IntArray,
            rodapeForcado: String? = null
        ) {
            val guardado = Guardados.mercadoGuardado(ctx)
            val btc = guardado.acharPor("bitcoin")
            val eth = guardado.acharPor("ethereum")
            val rv = RemoteViews(ctx.packageName, R.layout.widget_cotacao)

            if (btc != null) {
                rv.setTextViewText(R.id.wPreco, Formato.dinheiro(btc.preco))
                rv.setTextViewText(R.id.wVar, Formato.porcento(btc.variacao24h))
                rv.setTextColor(
                    R.id.wVar,
                    ctx.getColor(if (btc.variacao24h >= 0) R.color.alta else R.color.baixa)
                )
                rv.setTextViewText(
                    R.id.wEth,
                    eth?.let { "ETH ${Formato.dinheiro(it.preco)}  ${Formato.porcento(it.variacao24h)}" } ?: ""
                )
                rv.setTextViewText(
                    R.id.wRodape,
                    rodapeForcado ?: ctx.getString(R.string.widget_as, Formato.hora(guardado.atualizadoEm))
                )
            } else {
                // Primeiríssima vez, antes de qualquer busca terminar.
                rv.setTextViewText(R.id.wPreco, "—")
                rv.setTextViewText(R.id.wVar, "")
                rv.setTextViewText(R.id.wEth, "")
                rv.setTextViewText(R.id.wRodape, rodapeForcado ?: ctx.getString(R.string.widget_primeira_vez))
            }

            rv.setOnClickPendingIntent(R.id.wRaiz, abrirApp(ctx))
            rv.setOnClickPendingIntent(R.id.wAtualizar, forcarAtualizacao(ctx))
            ids.forEach { gerente.updateAppWidget(it, rv) }
        }

        private fun abrirApp(ctx: Context): PendingIntent {
            val i = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(ctx, 0, i, PendingIntent.FLAG_IMMUTABLE)
        }

        private fun forcarAtualizacao(ctx: Context): PendingIntent {
            val i = Intent(ctx, WidgetCotacao::class.java).setAction(ACAO_ATUALIZAR)
            return PendingIntent.getBroadcast(ctx, 1, i, PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
