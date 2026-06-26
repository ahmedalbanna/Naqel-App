package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkSapphireColorScheme = darkColorScheme(
    primary = SapphirePrimaryDark,
    onPrimary = SapphireOnPrimaryDark,
    primaryContainer = SapphirePrimaryContainerDark,
    onPrimaryContainer = SapphireOnPrimaryContainerDark,
    secondary = SapphireSecondaryDark,
    onSecondary = SapphireOnSecondaryDark,
    secondaryContainer = SapphireSecondaryContainerDark,
    onSecondaryContainer = SapphireOnSecondaryContainerDark,
    tertiary = SapphireTertiaryDark,
    onTertiary = SapphireOnTertiaryDark,
    tertiaryContainer = SapphireTertiaryContainerDark,
    onTertiaryContainer = SapphireOnTertiaryContainerDark,
    background = SapphireBackgroundDark,
    onBackground = SapphireOnBackgroundDark,
    surface = SapphireSurfaceDark,
    onSurface = SapphireOnSurfaceDark,
    surfaceVariant = SapphireSurfaceVariantDark,
    onSurfaceVariant = SapphireOnSurfaceVariantDark,
    error = SapphireErrorDark,
    onError = SapphireOnErrorDark,
    outline = SapphireOutlineDark
)

private val LightSapphireColorScheme = lightColorScheme(
    primary = SapphirePrimary,
    onPrimary = SapphireOnPrimary,
    primaryContainer = SapphirePrimaryContainer,
    onPrimaryContainer = SapphireOnPrimaryContainer,
    secondary = SapphireSecondary,
    onSecondary = SapphireOnSecondary,
    secondaryContainer = SapphireSecondaryContainer,
    onSecondaryContainer = SapphireOnSecondaryContainer,
    tertiary = SapphireTertiary,
    onTertiary = SapphireOnTertiary,
    tertiaryContainer = SapphireTertiaryContainer,
    onTertiaryContainer = SapphireOnTertiaryContainer,
    background = SapphireBackground,
    onBackground = SapphireOnBackground,
    surface = SapphireSurface,
    onSurface = SapphireOnSurface,
    surfaceVariant = SapphireSurfaceVariant,
    onSurfaceVariant = SapphireOnSurfaceVariant,
    error = SapphireError,
    onError = SapphireOnError,
    outline = SapphireOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkSapphireColorScheme else LightSapphireColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
