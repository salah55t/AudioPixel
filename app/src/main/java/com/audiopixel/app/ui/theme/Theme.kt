package com.audiopixel.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light palette
private val LightPrimary = Color(0xFF006DFF)
private val LightBackground = Color(0xFFF5F7FA)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnBackground = Color(0xFF1A1A1A)
private val LightOnSurface = Color(0xFF1A1A1A)
private val LightOnPrimary = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = NeonBlue,
    secondary = DeepPurple,
    tertiary = SuccessGreen,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = DarkBg,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = DeepPurple,
    tertiary = SuccessGreen,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = LightOnPrimary,
    onSecondary = LightOnPrimary,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    error = ErrorRed,
)

@Composable
fun AudioPixelTheme(
    darkTheme: Boolean = true, // افتراضياً: الثيم الداكن ليتناسب مع طابع التطبيق التقني
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
