package com.vasilev.beeropener.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimaryDark,
    onPrimary = OnAmberPrimaryDark,
    primaryContainer = AmberContainerDark,
    onPrimaryContainer = OnAmberContainerDark,
    secondary = SecondaryBrownDark,
    background = BackgroundTavernDark,
    surface = SurfaceTavernDark
)

private val LightColorScheme = lightColorScheme(
    primary = AmberPrimaryLight,
    onPrimary = OnAmberPrimaryLight,
    primaryContainer = AmberContainerLight,
    onPrimaryContainer = OnAmberContainerLight,
    secondary = SecondaryBrownLight,
    background = BackgroundCreamLight,
    surface = SurfaceWarmLight
)

@Composable
fun BeerOpenerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
