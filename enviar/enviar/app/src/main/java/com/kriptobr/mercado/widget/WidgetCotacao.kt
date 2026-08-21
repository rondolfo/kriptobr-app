package com.kriptobr.mercado.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kriptobr.mercado.MainActivity
import com.kriptobr.mercado.R
import com.kriptobr.mercado.alerta.VerificadorAlertas
import com.kriptobr.mercado.dados.Ajustes
import com.kriptobr.mercado.dados.Corretoras
import com.kriptobr.mercado.dados.Formato
import com.kriptobr.mercado.dados.Guardados
import com.kriptobr.mercado.dados.Idioma
import com.kriptobr.mercado.dados.Moeda

/**
 * Widget da tela inicial.
 *
 * Duas regras de ouro aqui:
 *
 * 1. **Nunca acessa a rede.** Só desenha o último preço guardado, que é sempre
 *    instantâneo. Quem busca da internet é o WorkManager, que pode demorar o
 *    quanto precisar. Foi a mistura das duas coisas que fazia aparecer "sem
 *    conexão" na primeira versão.
 * 2. **Nenhum texto vem do XML.** O launcher infla o layout no processo dele,
 *    com o idioma do aparelho — era por isso que o widget continuava em inglês
 *    com o app em português. Aqui todo texto é escrito por código, a partir de
 *    um contexto já ajustado para o idioma que a pessoa escolheu.
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

        /** Os cinco pares de linhas extras que existem no layout. */
        private val LINHAS = listOf(
            Triple(R.id.wL1, R.id.wL1Nome, R.id.wL1Var),
            Triple(R.id.wL2, R.id.wL2Nome, R.id.wL2Var),
            Triple(R.id.wL3, R.id.wL3Nome, R.id.wL3Var),
            Triple(R.id.wL4, R.id.wL4Nome, R.id.wL4Var),
            Triple(R.id.wL5, R.id.wL5Nome, R.id.wL5Var)
        )

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
            if (ids.isNotEmpty()) {
                val texto = Idioma.envolver(ctx).getString(R.string.widget_atualizando)
                desenhar(ctx, gerente, ids, texto)
            }
        }

        private fun desenhar(
            ctx: Context,
            gerente: AppWidgetManager,
            ids: IntArray,
            rodapeForcado: String? = null
        ) {
            /* Contexto no idioma escolhido — e moeda carregada, porque este
               processo pode ter acordado pelo widget, sem ninguém abrir a tela. */
            val c = Idioma.envolver(ctx)
            Ajustes.carregarNoProcesso(ctx)

            val guardado = Guardados.mercadoGuardado(ctx)
            val btc = guardado.acharPor("bitcoin")
            val rv = RemoteViews(ctx.packageName, R.layout.widget_cotacao)

            rv.setTextViewText(R.id.wMarca, c.getString(R.string.marca_curta))
            rv.setTextViewText(
                R.id.wTitulo,
                (btc?.nome ?: c.getString(R.string.widget_titulo)).uppercase()
            )

            if (btc == null) {
                // Primeiríssima vez, antes de qualquer busca terminar.
                rv.setTextViewText(R.id.wPreco, c.getString(R.string.travessao))
                rv.setTextViewText(R.id.wVar, "")
                rv.setViewVisibility(R.id.wFaixa, View.GONE)
                rv.setViewVisibility(R.id.wVolume, View.GONE)
                LINHAS.forEach { rv.setViewVisibility(it.first, View.GONE) }
                rv.setTextViewText(
                    R.id.wRodape,
                    rodapeForcado ?: c.getString(R.string.widget_primeira_vez)
                )
                ligarBotoes(ctx, rv)
                ids.forEach { gerente.updateAppWidget(it, rv) }
                return
            }

            rv.setTextViewText(R.id.wPreco, Formato.dinheiro(btc.preco))
            rv.setTextViewText(R.id.wVar, Formato.porcento(btc.variacao24h))
            rv.setTextColor(R.id.wVar, ctx.getColor(if (btc.variacao24h >= 0) R.color.alta else R.color.baixa))

            // máxima e mínima do dia
            val temFaixa = Ajustes.widgetFaixa(ctx) && btc.maxima24h > 0.0 && btc.minima24h > 0.0
            rv.setViewVisibility(R.id.wFaixa, if (temFaixa) View.VISIBLE else View.GONE)
            if (temFaixa) {
                rv.setTextViewText(
                    R.id.wFaixa,
                    c.getString(
                        R.string.widget_faixa,
                        Formato.dinheiro(btc.maxima24h), Formato.dinheiro(btc.minima24h)
                    )
                )
            }

            // volume negociado
            val temVolume = Ajustes.widgetVolume(ctx) && btc.volume24h > 0.0
            rv.setViewVisibility(R.id.wVolume, if (temVolume) View.VISIBLE else View.GONE)
            if (temVolume) {
                rv.setTextViewText(
                    R.id.wVolume,
                    c.getString(R.string.widget_volume, Formato.compacto(btc.volume24h))
                )
            }

            // as moedas extras que a pessoa escolheu mostrar
            val extras: List<Moeda> = guardado.moedas
                .filter { it.id != "bitcoin" }
                .take(Ajustes.widgetQuantas(ctx))
            LINHAS.forEachIndexed { i, (caixa, nome, variacao) ->
                val m = extras.getOrNull(i)
                rv.setViewVisibility(caixa, if (m == null) View.GONE else View.VISIBLE)
                if (m == null) return@forEachIndexed
                rv.setTextViewText(nome, "${m.simbolo}  ${Formato.dinheiro(m.preco)}")
                rv.setTextViewText(variacao, Formato.porcento(m.variacao24h))
                rv.setTextColor(variacao, ctx.getColor(if (m.variacao24h >= 0) R.color.alta else R.color.baixa))
            }

            /* No rodapé: de onde veio o preço e de quando ele é. Quem fixou uma
               corretora precisa lembrar disso ao olhar o número na tela inicial. */
            val hora = Formato.hora(guardado.atualizadoEm)
            val fonte = btc.fonte
            rv.setTextViewText(
                R.id.wRodape,
                rodapeForcado ?: if (fonte.isNotEmpty() && fonte != Corretoras.MEDIA) {
                    "${Corretoras.nomeDe(fonte)} · $hora"
                } else {
                    c.getString(R.string.widget_as, hora)
                }
            )

            ligarBotoes(ctx, rv)
            ids.forEach { gerente.updateAppWidget(it, rv) }
        }

        private fun ligarBotoes(ctx: Context, rv: RemoteViews) {
            rv.setOnClickPendingIntent(R.id.wRaiz, abrirApp(ctx))
            rv.setOnClickPendingIntent(R.id.wAtualizar, forcarAtualizacao(ctx))
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
