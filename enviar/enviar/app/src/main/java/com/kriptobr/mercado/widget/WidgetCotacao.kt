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
 * Widget da tela inicial, no formato de painel de corretora: marca, par, preço
 * grande e uma grade de seis números — venda, compra, variação, volume, máxima
 * e mínima.
 *
 * Três regras de ouro aqui:
 *
 * 1. **Nunca acessa a rede.** Só desenha o último preço guardado, que é sempre
 *    instantâneo. Quem busca da internet é o WorkManager, que pode demorar o
 *    quanto precisar. Foi a mistura das duas coisas que fazia aparecer "sem
 *    conexão" na primeira versão.
 * 2. **Nenhum texto vem do XML.** O launcher infla o layout no processo dele,
 *    com o idioma do aparelho — era por isso que o widget continuava em inglês
 *    com o app em português.
 * 3. **Não inventa número.** Venda e compra são o livro de ofertas de uma
 *    corretora; a média do mercado não tem livro. Sem corretora escolhida, essa
 *    coluna simplesmente sai da tela em vez de mostrar o preço repetido.
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

        /** As cinco linhas de moedas extras que existem no layout. */
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

        /** Escreve uma célula da grade: rótulo pequeno + valor. */
        private fun celula(rv: RemoteViews, rotulo: Int, valor: Int, texto: String?, numero: String) {
            rv.setTextViewText(rotulo, texto ?: "")
            rv.setTextViewText(valor, numero)
        }

        /* Números da grade saem sem "R$": o par no cabeçalho já diz a moeda, e
           cada caractere economizado é largura que o número não perde no corte.
           Acima de dez mil os centavos também caem — em "390.605,00" eles são
           ruído, e são justamente os três caracteres que faziam o valor sumir
           num widget de três células. */
        private fun seco(v: Double): String = when {
            v >= 10_000 -> Formato.numero(v, 0)
            v >= 1.0 -> Formato.numero(v, 2, 2)
            else -> Formato.numero(v, 6, 2)
        }

        private fun desenhar(
            ctx: Context,
            gerente: AppWidgetManager,
            ids: IntArray,
            avisoForcado: String? = null
        ) {
            /* Contexto no idioma escolhido — e moeda carregada, porque este
               processo pode ter acordado pelo widget, sem ninguém abrir a tela. */
            val c = Idioma.envolver(ctx)
            Ajustes.carregarNoProcesso(ctx)

            val guardado = Guardados.mercadoGuardado(ctx)
            val btc = guardado.acharPor("bitcoin")
            val fiat = Guardados.fiat().uppercase()
            val rv = RemoteViews(ctx.packageName, R.layout.widget_cotacao)

            rv.setImageViewResource(R.id.wLogo, R.drawable.logo_kriptobr)

            if (btc == null) {
                // Primeiríssima vez, antes de qualquer busca terminar.
                rv.setTextViewText(R.id.wPar, c.getString(R.string.widget_titulo))
                rv.setTextViewText(R.id.wPreco, c.getString(R.string.travessao))
                listOf(R.id.wColA, R.id.wColB, R.id.wColC).forEach { rv.setViewVisibility(it, View.GONE) }
                LINHAS.forEach { rv.setViewVisibility(it.first, View.GONE) }
                rv.setViewVisibility(R.id.wRodape, View.VISIBLE)
                rv.setTextViewText(
                    R.id.wRodape,
                    avisoForcado ?: c.getString(R.string.widget_primeira_vez)
                )
                ligarBotoes(ctx, rv)
                ids.forEach { gerente.updateAppWidget(it, rv) }
                return
            }

            // ------------------------------------------------------ cabeçalho
            val fonte = btc.fonte
            val nomeFonte = if (fonte.isNotEmpty() && fonte != Corretoras.MEDIA) {
                Corretoras.nomeDe(fonte)
            } else {
                c.getString(R.string.fonte_media_curta)
            }
            /* Só o par no cabeçalho. A corretora e a hora foram para o rodapé:
               num widget de três células, "BTC/BRL · Mercado Bitcoin" cortava o
               preço grande ao lado, que é o número que a pessoa quer ver. */
            rv.setTextViewText(R.id.wPar, "${btc.simbolo}/$fiat")
            /* Sem símbolo de moeda: o par ao lado já diz "BTC/BRL", e o espaço
               que o "R$" ocuparia vale mais para o número não ser cortado. */
            rv.setTextViewText(
                R.id.wPreco,
                if (btc.preco >= 1.0) Formato.numero(btc.preco, 2, 2) else Formato.numero(btc.preco, 6, 2)
            )


            // -------------------------------------- coluna A: livro de ofertas
            val temLivro = btc.venda > 0.0 && btc.compra > 0.0
            rv.setViewVisibility(R.id.wColA, if (temLivro) View.VISIBLE else View.GONE)
            if (temLivro) {
                celula(rv, R.id.wVendaL, R.id.wVendaV,
                    c.getString(R.string.rot_venda), seco(btc.venda))
                celula(rv, R.id.wCompraL, R.id.wCompraV,
                    c.getString(R.string.rot_compra), seco(btc.compra))
            }

            // ------------------------------ coluna B: variação e volume do dia
            /* Volta a mostrar: o launcher reaplica as ações sobre a árvore de
               views que já existe, então um GONE do desenho anterior (o da
               primeira vez, sem dado nenhum) sobreviveria para sempre. */
            rv.setViewVisibility(R.id.wColB, View.VISIBLE)
            celula(rv, R.id.wVarL, R.id.wVarV,
                c.getString(R.string.rot_24h), Formato.porcento(btc.variacao24h, comSeta = false)
                    .let { if (btc.variacao24h >= 0) "+$it" else "−$it" })
            rv.setTextColor(
                R.id.wVarV,
                ctx.getColor(if (btc.variacao24h >= 0) R.color.alta else R.color.baixa)
            )
            val temVolume = Ajustes.widgetVolume(ctx) && btc.volume24h > 0.0
            rv.setViewVisibility(R.id.wVolL, if (temVolume) View.VISIBLE else View.INVISIBLE)
            rv.setViewVisibility(R.id.wVolV, if (temVolume) View.VISIBLE else View.INVISIBLE)
            if (temVolume) {
                celula(rv, R.id.wVolL, R.id.wVolV,
                    c.getString(R.string.rot_vol), Formato.compacto(btc.volume24h, comMoeda = false))
            }

            // -------------------------------------- coluna C: máxima e mínima
            val temFaixa = Ajustes.widgetFaixa(ctx) && btc.maxima24h > 0.0 && btc.minima24h > 0.0
            rv.setViewVisibility(R.id.wColC, if (temFaixa) View.VISIBLE else View.GONE)
            if (temFaixa) {
                celula(rv, R.id.wMaxL, R.id.wMaxV,
                    c.getString(R.string.rot_max), seco(btc.maxima24h))
                celula(rv, R.id.wMinL, R.id.wMinV,
                    c.getString(R.string.rot_min), seco(btc.minima24h))
            }

            // ------------------------------------ as moedas extras escolhidas
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

            /* Rodapé: de onde veio o preço e de quando ele é. Enquanto não
               houver corretora fixada, ele empresta o espaço para dizer onde
               ligar venda e compra — e volta ao normal assim que ela existir. */
            val semCorretora = fonte.isEmpty() || fonte == Corretoras.MEDIA
            rv.setTextViewText(
                R.id.wRodape,
                avisoForcado
                    ?: if (semCorretora) c.getString(R.string.widget_sem_livro)
                    else "$nomeFonte · ${Formato.hora(guardado.atualizadoEm)}"
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
