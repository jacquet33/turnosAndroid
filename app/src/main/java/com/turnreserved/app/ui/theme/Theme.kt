package com.turnreserved.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF0069CC)
val PrimaryDark = Color(0xFF0055A4)
val Background = Color(0xFF000914)
val Surface = Color(0xFF001B3D)
val CardBg = Color(0x0AFFFFFF)
val CardBorder = Color(0x14FFFFFF)
val TextPrimary = Color.White
val TextSecondary = Color(0x80FFFFFF)
val TextTertiary = Color(0x4DFFFFFF)
val Success = Color(0xFF33C759)
val Warning = Color(0xFFFFBF00)
val Destructive = Color(0xFFEF4135)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    background = Background,
    surface = Surface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Destructive
)

@Composable
fun TurnReservedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
