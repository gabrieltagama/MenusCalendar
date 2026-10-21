package com.gabrieltagama.menuplanner.feature.settings.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gabrieltagama.menuplanner.feature.settings.ui.FileShareActions
import com.gabrieltagama.menuplanner.feature.settings.ui.SettingsScreenRoute

/**
 * Navigation graph of the settings feature (see SettingsRoute).
 */
fun NavGraphBuilder.settingsGraph(rememberFileShare: @Composable (SnackbarHostState) -> FileShareActions) {
    composable<SettingsRoute> { SettingsScreenRoute(rememberFileShare = rememberFileShare) }
}
