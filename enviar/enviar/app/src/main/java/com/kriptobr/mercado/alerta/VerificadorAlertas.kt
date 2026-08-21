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
import com.kriptobr.mercado.dados.Ajustes
import com.kriptobr.mercado.dados.Formato
import com.kriptobr.mercado.dados.Guardados
import com.kriptobr.mercado.dados.Historico
import com.kriptobr.mercado.dados.Idioma
import com.kriptobr.mercado.dados.Mercado
import com.kriptobr.mercado.dados.Noticias
import com.kriptobr.mercado.dados.Repo
import com.kriptobr.mercado.widget.WidgetCotacao
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * O motor de segundo plano do app. Roda a cada quinze minutos, mesmo com o app
 * fechado, e faz quatro coisas com uma única busca de preços:
 *
 * 1. dispara os alertas de preço que bateram;
 * 2. anota o preço das moedas com alerta de variação e dispara os que caíram
 *    ou subiram além do combinado;
 * 3. manda o resumo do mercado de manhã e o resumo de notícias de manhã e à
 *    noite — sem alarme exato, que no Android moderno é para despertador;
 * 4. redesenha o widget com o dado fresco.
 *
 * É este worker que faz a rede — nunca o widget. O Android dá dez segundos para
 * um widget terminar o que começou, e era exatamente aí que aparecia o
 * "sem conexão" na primeira versão.
 */
class VerificadorAlertas(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val mercado = Repo.carregar(ctx).getOrNull() ?: return Result.retry()

        conferirAlertas(ctx, mercado)
        runCatching { resumosDoDia(ctx, mercado) }

        WidgetCotacao.redesenharTodos(ctx)
        return Result.success()
    }

    // ------------------------------------------------------------- alertas
    private fun conferirAlertas(ctx: Context, mercado: Mercado) {
        val alertas = Guardados.alertas(ctx)

        /* Só as moedas com alerta de variação entram na fita de histórico —
           anotar as cem maiores encheria o disco de quem nem usa o recurso. */
        val comVariacao = alertas
            .filter { it.tipo == TipoAlerta.VARIACAO && it.ativo }
            .map { it.moedaId }.toSet()
        Historico.registrar(ctx, mercado.moedas, comVariacao)

        if (alertas.isEmpty()) return
        val c = Idioma.envolver(ctx)
        val agora = System.currentTimeMillis()

        val atualizados = alertas.map { a ->
            if (!a.ativo) return@map a
            val moeda = mercado.acharPor(a.moedaId) ?: return@map a

            when (a.tipo) {
                TipoAlerta.PRECO -> {
                    if (a.disparadoEm > 0L) return@map a          // já avisou; não repete
                    val bateu = if (a.acima) moeda.preco >= a.alvo else moeda.preco <= a.alvo
                    if (!bateu) return@map a
                    Avisos.mostrar(
                        ctx, Avisos.CANAL_ALERTAS,
                        c.getString(
                            if (a.acima) R.string.aviso_subiu else R.string.aviso_caiu,
                            a.nome, Formato.dinheiro(a.alvo)
                        ),
                        c.getString(R.string.aviso_corpo, a.simbolo, Formato.dinheiro(moeda.preco)),
                        LINK_ALERTA, a.id.toInt()
                    )
                    a.copy(ativo = false, disparadoEm = agora)
                }

                TipoAlerta.VARIACAO -> {
                    // fica quieto por seis horas depois de avisar, senão repetiria
                    // a cada quinze minutos enquanto a queda durasse
                    if (agora - a.disparadoEm < Alerta.DESCANSO_VARIACAO) return@map a
                    val antes = Historico.precoHa(ctx, a.moedaId, a.janelaHoras * 3600_000L)
                        ?: return@map a
                    if (antes <= 0.0) return@map a
                    val mudou = (moeda.preco - antes) / antes * 100.0
                    val bateu = if (a.acima) mudou >= a.alvo else mudou <= -a.alvo
                    if (!bateu) return@map a
                    Avisos.mostrar(
                        ctx, Avisos.CANAL_ALERTAS,
                        c.getString(
                            if (a.acima) R.string.aviso_disparou_alta else R.string.aviso_disparou_queda,
                            a.nome, Formato.porcento(abs(mudou), comSeta = false), a.janelaHoras
                        ),
                        c.getString(R.string.aviso_corpo, a.simbolo, Formato.dinheiro(moeda.preco)),
                        LINK_ALERTA, a.id.toInt()
                    )
                    a.copy(disparadoEm = agora)
                }
            }
        }
        if (atualizados != alertas) Guardados.salvarAlertas(ctx, atualizados)
    }

    // -------------------------------------------------------------- resumos
    private suspend fun resumosDoDia(ctx: Context, mercado: Mercado) {
        val janela = Ajustes.janelaAgora()
        if (janela == 0 || !Ajustes.podeMandarResumo(ctx, janela)) return
        val c = Idioma.envolver(ctx)

        // resumo do mercado: só de manhã, para criar o hábito sem virar barulho
        if (janela == 1 && Ajustes.avisoResumo(ctx)) {
            val btc = mercado.acharPor("bitcoin")
            if (btc != null) {
                val partes = mutableListOf(
                    "${btc.simbolo} ${Formato.dinheiro(btc.preco)} (${Formato.porcento(btc.variacao24h)})"
                )
                mercado.medo?.let { partes += c.getString(R.string.resumo_medo, it.valor) }
                mercado.acharPor("tether")?.let {
                    if (Guardados.fiat().equals("brl", true)) {
                        partes += c.getString(R.string.resumo_dolar, Formato.dinheiroCheio(it.preco))
                    }
                }
                Avisos.mostrar(
                    ctx, Avisos.CANAL_RESUMO,
                    c.getString(R.string.resumo_titulo),
                    partes.joinToString(" · "),
                    LINK_RESUMO, ID_RESUMO
                )
            }
        }

        // resumo de notícias: manhã e noite
        if (Ajustes.avisoNoticias(ctx)) {
            val corte = maxOf(Ajustes.ultimaNoticiaAvisada(ctx), Noticias.visto(ctx))
            val novas = runCatching { Noticias.ultimas(12) }.getOrDefault(emptyList())
                .filter { corte > 0L && it.quando > corte }
            if (novas.isNotEmpty()) {
                Avisos.mostrar(
                    ctx, Avisos.CANAL_NOTICIAS,
                    c.resources.getQuantityString(R.plurals.noticias_novas, novas.size, novas.size),
                    novas.take(3).joinToString("\n") { "• ${it.titulo}" },
                    LINK_NOTICIAS, ID_NOTICIAS
                )
                Ajustes.marcarNoticiaAvisada(ctx, novas.maxOf { it.quando })
            } else if (corte == 0L) {
                /* Primeira vez: não existe "novo" ainda. Fixa a marca agora para
                   o próximo resumo ter contra o que comparar. */
                runCatching { Noticias.ultimas(1) }.getOrNull()?.firstOrNull()
                    ?.let { Ajustes.marcarNoticiaAvisada(ctx, it.quando) }
            }
        }

        // marca a janela mesmo sem ter mandado nada: senão tentaria de novo em 15 min
        Ajustes.marcarResumoEnviado(ctx, janela)
    }

    companion object {
        private const val NOME = "kbr-alertas"
        private const val ID_RESUMO = 90001
        private const val ID_NOTICIAS = 90002
        private const val LINK_ALERTA = "https://mercado.kriptobr.com/?utm_source=app&utm_medium=alerta"
        private const val LINK_RESUMO = "https://mercado.kriptobr.com/?utm_source=app&utm_medium=resumo"
        private const val LINK_NOTICIAS = "https://kriptohoje.com/?utm_source=app&utm_medium=resumo"

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
