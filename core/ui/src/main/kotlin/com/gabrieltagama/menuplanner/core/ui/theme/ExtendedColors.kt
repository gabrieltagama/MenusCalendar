package com.gabrieltagama.menuplanner.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness

/**
 * Semantic colors outside the Material scheme. Heaviness always reads as a traffic light
 * (green, amber, red), whatever the color scheme in use.
 */
@Immutable
data class SemanticColor(val accent: Color, val container: Color, val onContainer: Color)

@Immutable
data class ExtendedColors(val veryLow: SemanticColor, val medium: SemanticColor, val veryHigh: SemanticColor)

internal val LightExtendedColors = ExtendedColors(
    veryLow = SemanticColor(accent = Color(0xFF2E7D32), container = Color(0xFFD7F0D4), onContainer = Color(0xFF0B3D10)),
    medium = SemanticColor(accent = Color(0xFFB26A00), container = Color(0xFFFFE3B8), onContainer = Color(0xFF4A2A00)),
    veryHigh = SemanticColor(accent = Color(0xFFC62828), container = Color(0xFFFFDAD6), onContainer = Color(0xFF5C0A0A))
)

internal val DarkExtendedColors = ExtendedColors(
    veryLow = SemanticColor(accent = Color(0xFF8FD694), container = Color(0xFF1F4D24), onContainer = Color(0xFFD7F0D4)),
    medium = SemanticColor(accent = Color(0xFFFFB952), container = Color(0xFF5C3B00), onContainer = Color(0xFFFFE3B8)),
    veryHigh = SemanticColor(accent = Color(0xFFFFB4AB), container = Color(0xFF7A1C17), onContainer = Color(0xFFFFDAD6))
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

@Composable
@ReadOnlyComposable
fun Heaviness.colors(): SemanticColor = with(LocalExtendedColors.current) {
    when (this@colors) {
        Heaviness.VERY_LOW -> veryLow
        Heaviness.MEDIUM -> medium
        Heaviness.VERY_HIGH -> veryHigh
    }
}
