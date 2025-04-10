package com.example.skycast.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun SkyCastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFBB86FC),
            secondary = Color(0xFF03DAC6),
            // ... other dark theme colors
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6200EE),
            secondary = Color(0xFF03DAC6),
            // ... other light theme colors
        )
    }

    val scaledTypography = Typography(
        displayLarge = MaterialTheme.typography.displayLarge.copy(
            fontSize = MaterialTheme.typography.displayLarge.fontSize * fontScale
        ),
        displayMedium = MaterialTheme.typography.displayMedium.copy(
            fontSize = MaterialTheme.typography.displayMedium.fontSize * fontScale
        ),
        displaySmall = MaterialTheme.typography.displaySmall.copy(
            fontSize = MaterialTheme.typography.displaySmall.fontSize * fontScale
        ),
        headlineLarge = MaterialTheme.typography.headlineLarge.copy(
            fontSize = MaterialTheme.typography.headlineLarge.fontSize * fontScale
        ),
        headlineMedium = MaterialTheme.typography.headlineMedium.copy(
            fontSize = MaterialTheme.typography.headlineMedium.fontSize * fontScale
        ),
        headlineSmall = MaterialTheme.typography.headlineSmall.copy(
            fontSize = MaterialTheme.typography.headlineSmall.fontSize * fontScale
        ),
        titleLarge = MaterialTheme.typography.titleLarge.copy(
            fontSize = MaterialTheme.typography.titleLarge.fontSize * fontScale
        ),
        titleMedium = MaterialTheme.typography.titleMedium.copy(
            fontSize = MaterialTheme.typography.titleMedium.fontSize * fontScale
        ),
        titleSmall = MaterialTheme.typography.titleSmall.copy(
            fontSize = MaterialTheme.typography.titleSmall.fontSize * fontScale
        ),
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(
            fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontScale
        ),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(
            fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale
        ),
        bodySmall = MaterialTheme.typography.bodySmall.copy(
            fontSize = MaterialTheme.typography.bodySmall.fontSize * fontScale
        ),
        labelLarge = MaterialTheme.typography.labelLarge.copy(
            fontSize = MaterialTheme.typography.labelLarge.fontSize * fontScale
        ),
        labelMedium = MaterialTheme.typography.labelMedium.copy(
            fontSize = MaterialTheme.typography.labelMedium.fontSize * fontScale
        ),
        labelSmall = MaterialTheme.typography.labelSmall.copy(
            fontSize = MaterialTheme.typography.labelSmall.fontSize * fontScale
        )
    )

    MaterialTheme(
        colorScheme = colors,
        typography = scaledTypography,
        content = content
    )
}