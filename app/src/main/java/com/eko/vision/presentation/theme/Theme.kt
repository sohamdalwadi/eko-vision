package com.eko.vision.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004E58),
    onPrimaryContainer = Color(0xFF8CEEFF),
    secondary = Color(0xFF76D1FF),
    onSecondary = Color(0xFF003549),
    tertiary = Color(0xFFFFD600),
    background = Color(0xFF101416),
    surface = Color(0xFF101416),
    onBackground = Color(0xFFE0E3E5),
    onSurface = Color(0xFFE0E3E5)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006876),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8CEEFF),
    onPrimaryContainer = Color(0xFF001F25),
    secondary = Color(0xFF00668B),
    onSecondary = Color.White,
    tertiary = Color(0xFF725C00),
    background = Color(0xFFFBFCFE),
    surface = Color(0xFFFBFCFE)
)

@Composable
fun EkoVisionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme // Defaults to dark HUD theme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
