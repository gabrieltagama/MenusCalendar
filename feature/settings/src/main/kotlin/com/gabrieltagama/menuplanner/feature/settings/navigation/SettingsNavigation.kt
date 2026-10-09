package com.gabrieltagama.menuplanner.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gabrieltagama.menuplanner.feature.settings.ui.SettingsScreenRoute
import kotlinx.serialization.Serializable

/**
 * Type-safe route and navigation graph of the settings feature.
 */
@Serializable
data object SettingsRoute

fun NavGraphBuilder.settingsGraph() {
    composable<SettingsRoute> { SettingsScreenRoute() }
}
