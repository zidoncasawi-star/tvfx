package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NetflixDarkColorScheme = darkColorScheme(
    primary = NetflixRed,
    onPrimary = Color.White,
    primaryContainer = NetflixRed.copy(alpha = 0.2f),
    onPrimaryContainer = Color.White,
    secondary = RatingGold,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TextMuted
)

@Composable
fun FlixTvTheme(
    accentColor: Color = NetflixRed,
    content: @Composable () -> Unit
) {
    val dynamicColorScheme = NetflixDarkColorScheme.copy(
        primary = accentColor,
        primaryContainer = accentColor.copy(alpha = 0.2f),
        onPrimary = Color.White,
        onPrimaryContainer = Color.White
    )
    MaterialTheme(
        colorScheme = dynamicColorScheme,
        typography = Typography,
        content = content
    )
}

