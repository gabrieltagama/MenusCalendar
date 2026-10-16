package com.gabrieltagama.menuplanner.core.cloud.ui

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.gabrieltagama.menuplanner.core.cloud.auth.AuthorizationStep
import com.gabrieltagama.menuplanner.core.cloud.auth.GoogleDriveAuthorizer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

/**
 * Remembers the action that links Google Drive: it asks Google for access, shows the consent
 * screen when needed and hands the granted access token to onResult, or null when the user
 * cancelled or the request failed. The authorizer comes from the Hilt graph, so the UI never builds
 * infrastructure by hand.
 */
@Composable
fun rememberGoogleDriveConnect(onResult: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val authorizer = remember(context) { context.googleDriveAuthorizer() }
    val currentOnResult by rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        currentOnResult(if (result.resultCode == Activity.RESULT_OK) authorizer.tokenFromConsent(result.data) else null)
    }
    return remember<() -> Unit>(authorizer, consentLauncher) {
        {
            scope.launch {
                when (val step = authorizer.request()) {
                    is AuthorizationStep.Granted -> currentOnResult(step.accessToken)
                    is AuthorizationStep.ConsentRequired -> consentLauncher.launch(IntentSenderRequest.Builder(step.intentSender).build())
                    AuthorizationStep.Failed -> currentOnResult(null)
                }
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface GoogleDriveAuthorizerEntryPoint {
    fun googleDriveAuthorizer(): GoogleDriveAuthorizer
}

private fun Context.googleDriveAuthorizer(): GoogleDriveAuthorizer =
    EntryPointAccessors.fromApplication(applicationContext, GoogleDriveAuthorizerEntryPoint::class.java).googleDriveAuthorizer()
