package com.gabrieltagama.menuplanner.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Typography: serif display, headline and title styles give the app a recipe-book feel, while
 * body and label styles keep the default sans serif for legibility.
 */
private val Base = Typography()

private fun TextStyle.asDisplay(weight: FontWeight = FontWeight.SemiBold): TextStyle =
    copy(fontFamily = FontFamily.Serif, fontWeight = weight)

internal val MenuPlannerTypography = Typography(
    displayLarge = Base.displayLarge.asDisplay(),
    displayMedium = Base.displayMedium.asDisplay(),
    displaySmall = Base.displaySmall.asDisplay(),
    headlineLarge = Base.headlineLarge.asDisplay(),
    headlineMedium = Base.headlineMedium.asDisplay(),
    headlineSmall = Base.headlineSmall.asDisplay(),
    titleLarge = Base.titleLarge.asDisplay(),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold)
)
