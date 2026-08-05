package com.example.tv.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val TvDarkColorScheme = darkColorScheme(
    primary = TvRed,
    onPrimary = TvTextWhite,
    secondary = TvRed,
    background = TvBg,
    onBackground = TvTextWhite,
    surface = TvPanel,
    onSurface = TvTextWhite,
    surfaceVariant = TvCard,
    onSurfaceVariant = TvTextGray,
    border = TvBorder
)

@Composable
fun FxTvTelevisionTheme(
    accentColor: Color = TvRed,
    content: @Composable () -> Unit
) {
    val scheme = TvDarkColorScheme.copy(
        primary = accentColor,
        secondary = accentColor
    )
    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}
