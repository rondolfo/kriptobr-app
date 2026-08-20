package com.kriptobr.mercado.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/* Paleta da marca KriptoBR — a mesma do painel na web. */
val Fundo = Color(0xFF091417)
val Superficie = Color(0xFF0F1E23)
val Superficie2 = Color(0xFF162A31)
val Borda = Color(0xFF26424C)
val Tinta = Color(0xFFEAF2F3)
val Tinta2 = Color(0xFFA6BAC0)
val Apagado = Color(0xFF7A9299)
val Mint = Color(0xFF00C69F)
val MintTinta = Color(0xFF052A23)
val Btc = Color(0xFFF7931A)
val Eth = Color(0xFF3B84C9)
val Alta = Color(0xFF2BD07E)
val Baixa = Color(0xFFF2707B)

private val esquema = darkColorScheme(
    primary = Mint,
    onPrimary = MintTinta,
    secondary = Mint,
    background = Fundo,
    onBackground = Tinta,
    surface = Superficie,
    onSurface = Tinta,
    surfaceVariant = Superficie2,
    onSurfaceVariant = Tinta2,
    outline = Borda,
    error = Baixa
)

private val tipos = Typography(
    displayLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
    titleLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 14.sp),
    bodyMedium = TextStyle(fontSize = 13.sp),
    bodySmall = TextStyle(fontSize = 11.5f.sp),
    labelSmall = TextStyle(fontSize = 10.5f.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp)
)

@Composable
fun TemaKriptoBR(conteudo: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquema, typography = tipos, content = conteudo)
}

/** Cor de fundo do símbolo de cada moeda, estável pelo nome. */
fun corDaMoeda(simbolo: String): Color = when (simbolo.uppercase()) {
    "BTC" -> Btc
    "ETH" -> Eth
    "USDT", "USDC" -> Color(0xFF0E9E82)
    "SOL" -> Color(0xFF8A63D0)
    "XRP" -> Color(0xFFC9564B)
    "DOGE" -> Color(0xFFB08A2E)
    "BNB" -> Color(0xFFE0B23C)
    "ADA" -> Color(0xFF2E8FA6)
    else -> {
        var h = 0
        simbolo.forEach { h = (h * 31 + it.code) }
        listOf(
            Color(0xFF5B7FD0), Color(0xFF2E8FA6), Color(0xFF8A63D0),
            Color(0xFFB08A2E), Color(0xFF0E9E82), Color(0xFFC9564B)
        )[kotlin.math.abs(h) % 6]
    }
}
