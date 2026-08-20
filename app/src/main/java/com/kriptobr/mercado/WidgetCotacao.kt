package com.kriptobr.mercado

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Widget da tela inicial com o preço do Bitcoin (e do Ethereum, quando cabe).
 * O sistema chama onUpdate a cada 30 minutos — é o intervalo mínimo que o
 * Android aceita para não gastar bateria. Tocar no widget atualiza na hora.
 */
class WidgetCotacao : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, gerente: AppWidgetManager, ids: IntArray) {
        atualizar(ctx, gerente, ids)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACAO_ATUALIZAR) {
            val gerente = AppWidgetManager.getInstance(ctx)
            val ids = gerente.getAppWidgetIds(ComponentName(ctx, WidgetCotacao::class.java))
            atualizar(ctx, gerente, ids)
        }
    }

    private fun atualizar(ctx: Context, gerente: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val pendente = goAsync()                       // segura o processo vivo enquanto a rede responde
        Thread {
            val local = Locale.getDefault()
            val fiat = if (local.country.equals("BR", true)) "brl" else "usd"
            val resultado = runCatching { Cotacao.buscar(fiat) }
            val agora = SimpleDateFormat("HH:mm", local).format(Date())

            val rv = RemoteViews(ctx.packageName, R.layout.widget_cotacao)
            resultado.onSuccess { moedas ->
                val btc = moedas.firstOrNull { it.simbolo == "BTC" }
                val eth = moedas.firstOrNull { it.simbolo == "ETH" }
                if (btc != null) {
                    rv.setTextViewText(R.id.wPreco, Cotacao.formatarPreco(btc.preco, fiat, local))
                    rv.setTextViewText(R.id.wVar, Cotacao.formatarVariacao(btc.variacao24h, local))
                    rv.setTextColor(
                        R.id.wVar,
                        ctx.getColor(if (btc.variacao24h >= 0) R.color.alta else R.color.baixa)
                    )
                }
                rv.setTextViewText(
                    R.id.wEth,
                    eth?.let { "ETH ${Cotacao.formatarPreco(it.preco, fiat, local)}" } ?: ""
                )
                rv.setTextViewText(R.id.wRodape, ctx.getString(R.string.widget_atualizado, agora))
            }.onFailure {
                rv.setTextViewText(R.id.wVar, ctx.getString(R.string.widget_sem_conexao))
                rv.setTextColor(R.id.wVar, ctx.getColor(R.color.tinta2))
                rv.setTextViewText(R.id.wRodape, ctx.getString(R.string.widget_toque_atualizar))
            }

            // toque no corpo abre o painel; toque no rodapé força atualização
            rv.setOnClickPendingIntent(R.id.wRaiz, abrirApp(ctx))
            rv.setOnClickPendingIntent(R.id.wRodape, forcarAtualizacao(ctx))
            ids.forEach { gerente.updateAppWidget(it, rv) }
            pendente.finish()
        }.start()
    }

    private fun abrirApp(ctx: Context): PendingIntent {
        val i = Intent(ctx, TelaPrincipal::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(ctx, 0, i, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun forcarAtualizacao(ctx: Context): PendingIntent {
        val i = Intent(ctx, WidgetCotacao::class.java).setAction(ACAO_ATUALIZAR)
        return PendingIntent.getBroadcast(ctx, 1, i, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val ACAO_ATUALIZAR = "com.kriptobr.mercado.ATUALIZAR_WIDGET"
    }
}
