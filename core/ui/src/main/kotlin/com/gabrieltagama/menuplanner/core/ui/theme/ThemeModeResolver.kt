package com.gabrieltagama.menuplanner.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode

/**
 * Resolves the user's theme mode to dark or light colors; SYSTEM follows the device setting.
 */
@Composable
fun ThemeMode.isDarkTheme(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
