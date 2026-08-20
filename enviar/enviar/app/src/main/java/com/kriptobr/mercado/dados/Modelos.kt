package com.kriptobr.mercado.dados

/** Uma criptomoeda como o app precisa exibir. */
data class Moeda(
    val id: String,
    val simbolo: String,
    val nome: String,
    val preco: Double,
    val variacao24h: Double,
    val capMercado: Double = 0.0,
    val historico: List<Double> = emptyList()
)

/** Leitura do índice de medo e ganância. */
data class MedoGanancia(val valor: Int, val ontem: Int?, val semana: Int?)

/** Tudo que a tela principal precisa, junto, para desenhar de uma vez. */
data class Mercado(
    val moedas: List<Moeda> = emptyList(),
    val medo: MedoGanancia? = null,
    val atualizadoEm: Long = 0L
) {
    fun acharPor(id: String): Moeda? = moedas.firstOrNull { it.id == id }
}
