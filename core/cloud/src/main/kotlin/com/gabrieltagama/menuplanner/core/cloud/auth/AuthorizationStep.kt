package com.gabrieltagama.menuplanner.core.cloud.auth

import android.content.IntentSender

/**
 * Outcome of asking Google for access to the Drive app data folder: a usable token, a consent
 * screen the UI must launch, or a failure.
 */
sealed interface AuthorizationStep {
    data class Granted(val accessToken: String) : AuthorizationStep
    data class ConsentRequired(val intentSender: IntentSender) : AuthorizationStep
    data object Failed : AuthorizationStep
}
