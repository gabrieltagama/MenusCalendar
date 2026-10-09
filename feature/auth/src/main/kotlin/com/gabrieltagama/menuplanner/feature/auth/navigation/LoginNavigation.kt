package com.gabrieltagama.menuplanner.feature.auth.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gabrieltagama.menuplanner.feature.auth.ui.LoginScreenRoute
import kotlinx.serialization.Serializable

/**
 * Public navigation contract of the auth feature.
 */
@Serializable
data object LoginRoute

fun NavGraphBuilder.loginScreen(onLoggedIn: () -> Unit) = composable<LoginRoute> {
    LoginScreenRoute(onLoggedIn = onLoggedIn)
}
