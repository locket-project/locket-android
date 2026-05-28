package com.inhoolee.locket.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.inhoolee.locket.domain.ThemeMode

@Composable
fun ThemeModeSelector(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var isOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { isOpen = true }) {
            Icon(themeMode.icon(), contentDescription = "Theme")
        }
        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { isOpen = false }
        ) {
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label()) },
                    leadingIcon = { Icon(mode.icon(), contentDescription = null) },
                    trailingIcon = {
                        if (themeMode == mode) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        isOpen = false
                        onThemeModeChange(mode)
                    }
                )
            }
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.System -> "System"
    ThemeMode.Light -> "Light"
    ThemeMode.Dark -> "Dark"
}

private fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.System -> Icons.Default.BrightnessAuto
    ThemeMode.Light -> Icons.Default.LightMode
    ThemeMode.Dark -> Icons.Default.DarkMode
}
