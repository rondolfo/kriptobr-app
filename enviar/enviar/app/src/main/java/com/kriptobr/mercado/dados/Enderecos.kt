package com.kriptobr.mercado.dados

/** Uma possibilidade de rede para o que a pessoa colou. */
data class Destino(
    val rede: String,
    val moeda: String,
    val url: String,
    val tipo: Tipo
) {
    enum class Tipo { CARTEIRA, TRANSACAO }
}

/**
 * Descobre a que rede pertence um endereço ou hash colado, só olhando o formato.
 *
 * Nada aqui vai à internet: cada rede tem um desenho de endereço próprio e isso
 * basta para identificar. A exceção é o endereço que começa com 0x — ele é
 * literalmente o mesmo na Ethereum, na BNB Chain, na Polygon e nas outras redes
 * compatíveis, então é impossível adivinhar, e o honesto é mostrar as opções em
 * vez de escolher por conta própria e mandar a pessoa para o lugar errado.
 */
object Enderecos {

    private val BASE58 = Regex("^[1-9A-HJ-NP-Za-km-z]+$")
    private val HEX = Regex("^[0-9a-fA-F]+$")
    private val BECH32 = Regex("^[a-z0-9]+$")

    private fun evm(nome: String, moeda: String, base: String, valor: String, transacao: Boolean) =
        Destino(nome, moeda, base + (if (transacao) "/tx/" else "/address/") + valor,
            if (transacao) Destino.Tipo.TRANSACAO else Destino.Tipo.CARTEIRA)

    /** Devolve os destinos possíveis, do mais provável para o menos. Vazio = não reconhecido. */
    fun identificar(bruto: String): List<Destino> {
        val v = bruto.trim().removePrefix("bitcoin:").removePrefix("ethereum:").trim()
        if (v.isEmpty()) return emptyList()
        val min = v.lowercase()

        // ---------- redes compatíveis com a Ethereum ----------
        if (min.startsWith("0x")) {
            val corpo = v.substring(2)
            if (corpo.length == 40 && HEX.matches(corpo)) return redesEvm(v, false)
            if (corpo.length == 64 && HEX.matches(corpo)) return redesEvm(v, true)
            return emptyList()
        }

        // ---------- Bitcoin ----------
        if (v.length in 26..35 && (v.startsWith("1") || v.startsWith("3")) && BASE58.matches(v))
            return listOf(Destino("Bitcoin", "BTC", "https://mempool.space/address/$v", Destino.Tipo.CARTEIRA))
        if (min.startsWith("bc1") && min.length in 42..62 && BECH32.matches(min))
            return listOf(Destino("Bitcoin", "BTC", "https://mempool.space/address/$min", Destino.Tipo.CARTEIRA))

        // ---------- Litecoin ----------
        if (min.startsWith("ltc1") && BECH32.matches(min))
            return listOf(Destino("Litecoin", "LTC", "https://blockchair.com/litecoin/address/$min", Destino.Tipo.CARTEIRA))
        if (v.length in 26..36 && (v.startsWith("L") || v.startsWith("M")) && BASE58.matches(v))
            return listOf(Destino("Litecoin", "LTC", "https://blockchair.com/litecoin/address/$v", Destino.Tipo.CARTEIRA))

        // ---------- Dogecoin ----------
        if (v.length in 33..35 && v.startsWith("D") && BASE58.matches(v))
            return listOf(Destino("Dogecoin", "DOGE", "https://blockchair.com/dogecoin/address/$v", Destino.Tipo.CARTEIRA))

        // ---------- TRON ----------
        if (v.length == 34 && v.startsWith("T") && BASE58.matches(v))
            return listOf(Destino("TRON", "TRX", "https://tronscan.org/#/address/$v", Destino.Tipo.CARTEIRA))

        // ---------- XRP ----------
        if (v.length in 25..35 && v.startsWith("r") && BASE58.matches(v))
            return listOf(Destino("XRP Ledger", "XRP", "https://xrpscan.com/account/$v", Destino.Tipo.CARTEIRA))

        // ---------- Cardano ----------
        if (min.startsWith("addr1") || min.startsWith("stake1"))
            return listOf(Destino("Cardano", "ADA", "https://cardanoscan.io/address/$v", Destino.Tipo.CARTEIRA))

        // ---------- Cosmos e Atom ----------
        if (min.startsWith("cosmos1"))
            return listOf(Destino("Cosmos", "ATOM", "https://www.mintscan.io/cosmos/address/$v", Destino.Tipo.CARTEIRA))

        // ---------- hash de 64 caracteres sem 0x ----------
        if (v.length == 64 && HEX.matches(v)) return listOf(
            Destino("Bitcoin", "BTC", "https://mempool.space/tx/$v", Destino.Tipo.TRANSACAO),
            Destino("TRON", "TRX", "https://tronscan.org/#/transaction/$v", Destino.Tipo.TRANSACAO),
            Destino("Cardano", "ADA", "https://cardanoscan.io/transaction/$v", Destino.Tipo.TRANSACAO),
            Destino("XRP Ledger", "XRP", "https://xrpscan.com/tx/$v", Destino.Tipo.TRANSACAO)
        )

        // ---------- Solana (por último: o formato é o mais aberto de todos) ----------
        if (v.length in 43..44 && BASE58.matches(v))
            return listOf(Destino("Solana", "SOL", "https://solscan.io/account/$v", Destino.Tipo.CARTEIRA))
        if (v.length in 86..90 && BASE58.matches(v))
            return listOf(Destino("Solana", "SOL", "https://solscan.io/tx/$v", Destino.Tipo.TRANSACAO))
        if (v.length in 32..44 && BASE58.matches(v))
            return listOf(Destino("Solana", "SOL", "https://solscan.io/account/$v", Destino.Tipo.CARTEIRA))

        return emptyList()
    }

    private fun redesEvm(v: String, transacao: Boolean) = listOf(
        evm("Ethereum", "ETH", "https://etherscan.io", v, transacao),
        evm("BNB Chain", "BNB", "https://bscscan.com", v, transacao),
        evm("Polygon", "POL", "https://polygonscan.com", v, transacao),
        evm("Arbitrum", "ARB", "https://arbiscan.io", v, transacao),
        evm("Base", "ETH", "https://basescan.org", v, transacao),
        evm("Optimism", "OP", "https://optimistic.etherscan.io", v, transacao),
        evm("Avalanche", "AVAX", "https://snowtrace.io", v, transacao)
    )
}
