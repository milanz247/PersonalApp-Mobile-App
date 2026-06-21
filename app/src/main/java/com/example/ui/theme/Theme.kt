package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

@Composable
fun getAppGradient(darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme()): Brush {
    if (darkTheme) {
        return Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A), // Tailwind Slate 900
                Color(0xFF020617)  // Tailwind Slate 950
            )
        )
    }
    // Elegant, clean gradient starting with pure white and settling on soft Facebook grey
    return Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF0F2F5)
        )
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    background = DarkBackground,
    onBackground = DarkForeground,
    surface = DarkCardBackground,
    onSurface = DarkForeground,
    surfaceVariant = DarkMutedBackground,
    onSurfaceVariant = DarkMutedText,
    outline = DarkCardBorder,
    error = SystemRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    background = LightBackground,
    onBackground = LightForeground,
    surface = LightCardBackground,
    onSurface = LightForeground,
    surfaceVariant = LightMutedBackground,
    onSurfaceVariant = LightMutedText,
    outline = LightCardBorder,
    error = SystemRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
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
