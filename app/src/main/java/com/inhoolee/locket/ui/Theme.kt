package com.inhoolee.locket.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LocketColorScheme = lightColorScheme(
    primary = Color(0xFF2F5F4A),
    onPrimary = Color.White,
    secondary = Color(0xFF7A5B36),
    background = Color(0xFFFAF7F1),
    surface = Color(0xFFFFFCF7),
    surfaceVariant = Color(0xFFEDE4D8),
    onSurface = Color(0xFF24211D)
)

@Composable
fun LocketTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LocketColorScheme,
        content = content
    )
}
