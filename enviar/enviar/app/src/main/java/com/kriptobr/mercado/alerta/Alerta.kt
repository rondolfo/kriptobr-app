package com.kriptobr.mercado.alerta

import org.json.JSONObject

/** Alerta de preço criado pelo próprio usuário. Fica só no aparelho dele. */
data class Alerta(
    val id: Long,
    val moedaId: String,
    val simbolo: String,
    val nome: String,
    val acima: Boolean,
    val alvo: Double,
    val ativo: Boolean = true,
    val disparadoEm: Long = 0L
) {
    fun paraJson(): JSONObject = JSONObject().apply {
        put("id", id); put("moedaId", moedaId); put("simbolo", simbolo); put("nome", nome)
        put("acima", acima); put("alvo", alvo); put("ativo", ativo); put("disparado", disparadoEm)
    }

    companion object {
        fun deJson(o: JSONObject) = Alerta(
            id = o.getLong("id"),
            moedaId = o.getString("moedaId"),
            simbolo = o.getString("simbolo"),
            nome = o.optString("nome", o.getString("simbolo")),
            acima = o.getBoolean("acima"),
            alvo = o.getDouble("alvo"),
            ativo = o.optBoolean("ativo", true),
            disparadoEm = o.optLong("disparado", 0L)
        )
    }
}
