package com.inhoolee.locket.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.inhoolee.locket.domain.ThemeMode

private val LightLocketColorScheme = lightColorScheme(
    primary = Color(0xFF2F5F4A),
    onPrimary = Color.White,
    secondary = Color(0xFF7A5B36),
    background = Color(0xFFFAF7F1),
    surface = Color(0xFFFFFCF7),
    surfaceVariant = Color(0xFFEDE4D8),
    onSurface = Color(0xFF24211D),
    onSurfaceVariant = Color(0xFF5E5348)
)

private val DarkLocketColorScheme = darkColorScheme(
    primary = Color(0xFF9BD7B9),
    onPrimary = Color(0xFF083823),
    secondary = Color(0xFFE4C18D),
    background = Color(0xFF14110F),
    surface = Color(0xFF1C1814),
    surfaceVariant = Color(0xFF332D26),
    onSurface = Color(0xFFF0E8DE),
    onSurfaceVariant = Color(0xFFD7C7B8)
)

private val LocalLocketDarkTheme = staticCompositionLocalOf { false }

@Composable
fun LocketTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val colorScheme = if (useDarkTheme) DarkLocketColorScheme else LightLocketColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val barColor = colorScheme.background.toArgb()

            window.statusBarColor = barColor
            window.navigationBarColor = barColor
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    CompositionLocalProvider(LocalLocketDarkTheme provides useDarkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

@Composable
fun isLocketDarkTheme(): Boolean = LocalLocketDarkTheme.current

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
