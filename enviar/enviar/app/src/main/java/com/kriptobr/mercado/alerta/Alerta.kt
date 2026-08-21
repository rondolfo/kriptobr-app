package com.kriptobr.mercado.alerta

import org.json.JSONObject

/** O que dispara o aviso. */
enum class TipoAlerta { PRECO, VARIACAO }

/**
 * Alerta criado pelo próprio usuário. Fica só no aparelho dele — nada é enviado
 * para servidor nenhum.
 *
 * São dois feitios:
 *
 * - **PRECO** — "me avise quando o Bitcoin passar de R$ 400 mil". Dispara uma
 *   vez e se desliga; repetir seria spam, porque o preço fica oscilando em
 *   volta do alvo.
 * - **VARIACAO** — "me avise se cair mais de 5% em uma hora". Esse não se
 *   desliga: o que interessa é o próximo tombo, não o primeiro. Ele só fica
 *   quieto por seis horas depois de disparar, senão avisaria a cada quinze
 *   minutos enquanto a queda durasse.
 */
data class Alerta(
    val id: Long,
    val moedaId: String,
    val simbolo: String,
    val nome: String,
    /** PRECO: acima do alvo. VARIACAO: alta (false = queda). */
    val acima: Boolean,
    /** PRECO: preço em reais/dólares. VARIACAO: percentual, ex.: 5.0. */
    val alvo: Double,
    val ativo: Boolean = true,
    val disparadoEm: Long = 0L,
    val tipo: TipoAlerta = TipoAlerta.PRECO,
    /** Só para VARIACAO: 1 ou 24 horas. */
    val janelaHoras: Int = 1
) {
    fun paraJson(): JSONObject = JSONObject().apply {
        put("id", id); put("moedaId", moedaId); put("simbolo", simbolo); put("nome", nome)
        put("acima", acima); put("alvo", alvo); put("ativo", ativo); put("disparado", disparadoEm)
        put("tipo", tipo.name); put("janela", janelaHoras)
    }

    companion object {
        /** Espera antes de o alerta de variação poder avisar de novo. */
        const val DESCANSO_VARIACAO = 6L * 3600 * 1000

        fun deJson(o: JSONObject) = Alerta(
            id = o.getLong("id"),
            moedaId = o.getString("moedaId"),
            simbolo = o.getString("simbolo"),
            nome = o.optString("nome", o.getString("simbolo")),
            acima = o.getBoolean("acima"),
            alvo = o.getDouble("alvo"),
            ativo = o.optBoolean("ativo", true),
            disparadoEm = o.optLong("disparado", 0L),
            // alertas gravados pela versão anterior não têm tipo: são de preço
            tipo = runCatching { TipoAlerta.valueOf(o.optString("tipo", "PRECO")) }
                .getOrDefault(TipoAlerta.PRECO),
            janelaHoras = o.optInt("janela", 1)
        )
    }
}
