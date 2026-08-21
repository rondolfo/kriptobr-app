package com.kriptobr.mercado.dados

/** Uma criptomoeda como o app precisa exibir. */
data class Moeda(
    val id: String,
    val simbolo: String,
    val nome: String,
    val preco: Double,
    val variacao24h: Double,
    val capMercado: Double = 0.0,
    /* Máxima, mínima e volume das últimas 24 h. Zero quer dizer "não veio",
       não "foi zero" — a tela some com o campo em vez de mostrar R$ 0,00. */
    val maxima24h: Double = 0.0,
    val minima24h: Double = 0.0,
    val volume24h: Double = 0.0,
    /* Livro de ofertas da corretora escolhida: venda = quanto custa comprar
       agora, compra = quanto pagam para tirar de você. Zero quando o preço vem
       da média do mercado, que não tem livro. */
    val venda: Double = 0.0,
    val compra: Double = 0.0,
    val historico: List<Double> = emptyList(),
    /** Corretora de onde veio este preço. Vazio = média do mercado. */
    val fonte: String = "",
    /** true quando o preço da corretora era em dólar e foi convertido para real. */
    val convertido: Boolean = false
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
