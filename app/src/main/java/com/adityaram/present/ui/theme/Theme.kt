package com.adityaram.present.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class PresentColors(
    val background: Color,
    val surface: Color,
    val elevatedSurface: Color,
    val border: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val accent: Color,
    val softAccent: Color,
    val safe: Color,
    val warning: Color,
    val critical: Color
)

val LocalPresentColors = staticCompositionLocalOf<PresentColors> {
    error("No PresentColors provided")
}

val PresentDarkColors = PresentColors(
    background = DarkBackground,
    surface = DarkSurface,
    elevatedSurface = DarkElevatedSurface,
    border = DarkBorder,
    primaryText = DarkPrimaryText,
    secondaryText = DarkSecondaryText,
    mutedText = DarkMutedText,
    accent = DarkAccent,
    softAccent = DarkSoftAccent,
    safe = DarkSafe,
    warning = DarkWarning,
    critical = DarkCritical
)

val PresentLightColors = PresentColors(
    background = LightBackground,
    surface = LightSurface,
    elevatedSurface = LightElevatedSurface,
    border = LightBorder,
    primaryText = LightPrimaryText,
    secondaryText = LightSecondaryText,
    mutedText = LightMutedText,
    accent = LightAccent,
    softAccent = LightSoftAccent,
    safe = LightSafe,
    warning = LightWarning,
    critical = LightCritical
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkPrimaryText,
    onBackground = DarkPrimaryText,
    onSurface = DarkPrimaryText,
    error = DarkCritical
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = LightSurface,
    onBackground = LightPrimaryText,
    onSurface = LightPrimaryText,
    error = LightCritical
)

// Default darkTheme = true to satisfy requirement "First launch must default to Dark Mode" 
// In the production app, this parameter will be populated dynamically from DataStore
@Composable
fun PresentTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val presentColors = if (darkTheme) PresentDarkColors else PresentLightColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalPresentColors provides presentColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
