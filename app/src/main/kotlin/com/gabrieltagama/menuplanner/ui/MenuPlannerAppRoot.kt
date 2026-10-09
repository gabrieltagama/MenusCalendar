package com.gabrieltagama.menuplanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.gabrieltagama.menuplanner.AppViewModel
import com.gabrieltagama.menuplanner.core.domain.model.AuthState
import com.gabrieltagama.menuplanner.feature.auth.navigation.LoginRoute
import com.gabrieltagama.menuplanner.feature.auth.navigation.loginScreen

/**
 * Root composable. The authentication state is the single source of truth: while
 * unauthenticated only the login graph exists, so logging out returns to it automatically and
 * the main back stack is discarded.
 */
@Composable
fun MenuPlannerAppRoot(viewModel: AppViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    when (authState) {
        AuthState.Unauthenticated -> LoginHost()
        AuthState.Authenticated -> MainShell(onLogout = viewModel::logout)
    }
}

@Composable
private fun LoginHost() = NavHost(navController = rememberNavController(), startDestination = LoginRoute) {
    loginScreen(onLoggedIn = {})
}
