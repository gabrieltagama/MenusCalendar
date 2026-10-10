package com.gabrieltagama.menuplanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.AppViewModel
import com.gabrieltagama.menuplanner.feature.onboarding.ui.WelcomeScreenRoute

/**
 * Root composable. There is no login: on the first start the welcome screen offers linking
 * Google for the recipe backup or using the app locally; once answered the app always opens the
 * main shell directly.
 */
@Composable
fun MenuPlannerAppRoot(viewModel: AppViewModel = hiltViewModel()) {
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    if (isOnboardingCompleted) MainShell()
    else WelcomeScreenRoute()
}
