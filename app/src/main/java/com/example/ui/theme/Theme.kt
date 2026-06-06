package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicColorScheme = lightColorScheme(
    primary = ElectroCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = ElectroCyan,
    secondary = ElectroCyan,
    onSecondary = Color.White,
    background = DarkSlate,
    onBackground = TextLight,
    surface = SlateCard,
    onSurface = TextLight,
    error = LaserMagenta
)

// Define local light theme for completeness
private val LightCosmicColorScheme = CosmicColorScheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our beautiful Cosmic Cyber brand perfectly
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CosmicColorScheme else LightCosmicColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
