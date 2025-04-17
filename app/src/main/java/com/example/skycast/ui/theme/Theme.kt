package com.example.skycast.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalFontScale = compositionLocalOf { 1.0f }

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun SkyCastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = LocalFontScale.current,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFBB86FC),
            secondary = Color(0xFF03DAC6)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6200EE),
            secondary = Color(0xFF03DAC6)
        )
    }

    val scaledTypography = Typography(
        displayLarge = MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * fontScale),
        displayMedium = MaterialTheme.typography.displayMedium.copy(fontSize = MaterialTheme.typography.displayMedium.fontSize * fontScale),
        displaySmall = MaterialTheme.typography.displaySmall.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize * fontScale),
        headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontSize = MaterialTheme.typography.headlineLarge.fontSize * fontScale),
        headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize * fontScale),
        headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize * fontScale),
        titleLarge = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize * fontScale),
        titleMedium = MaterialTheme.typography.titleMedium.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize * fontScale),
        titleSmall = MaterialTheme.typography.titleSmall.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize * fontScale),
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontScale),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale),
        bodySmall = MaterialTheme.typography.bodySmall.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize * fontScale),
        labelLarge = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize * fontScale),
        labelMedium = MaterialTheme.typography.labelMedium.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize * fontScale),
        labelSmall = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * fontScale)
    )

    // Trigger recomposition when fontScale changes
    LaunchedEffect(fontScale) {
        // Empty block to force recomposition
    }

    CompositionLocalProvider(LocalFontScale provides fontScale) {
        MaterialTheme(
            colorScheme = colors,
            typography = scaledTypography,
            content = content
        )
    }
}