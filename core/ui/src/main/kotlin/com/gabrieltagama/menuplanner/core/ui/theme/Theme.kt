package com.gabrieltagama.menuplanner.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

/**
 * App theme. Uses the MenuPlanner brand palette by default; [dynamicColor] switches to the
 * Material You colors of the wallpaper (always available, minSdk is 31). Semantic colors
 * (heaviness) stay the same in both cases.
 */
@Composable
fun MenuPlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = colorSchemeOf(darkTheme = darkTheme, dynamicColor = dynamicColor)
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MenuPlannerTypography,
            shapes = MenuPlannerShapes,
            content = content
        )
    }
}

@Composable
private fun colorSchemeOf(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme {
    if (!dynamicColor) return if (darkTheme) DarkColors else LightColors
    val context = LocalContext.current
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
