package ru.edgarakert.biblenote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Amber,
    onPrimary = Parchment,
    primaryContainer = AmberSoft,
    onPrimaryContainer = Ink,
    secondary = WarmGray,
    onSecondary = Parchment,
    secondaryContainer = Hairline,
    onSecondaryContainer = Ink,
    background = Parchment,
    onBackground = Ink,
    surface = CardSurface,
    onSurface = Ink,
    surfaceVariant = CardSurface,
    onSurfaceVariant = WarmGray,
    outline = Hairline,
    outlineVariant = Hairline,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAmber,
    onPrimary = DarkBackground,
    primaryContainer = DarkSurface,
    onPrimaryContainer = DarkInk,
    secondary = DarkWarmGray,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = DarkInk,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkWarmGray,
    outline = DarkHairline,
    outlineVariant = DarkHairline,
)

@Composable
fun BibleNoteTheme(
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
