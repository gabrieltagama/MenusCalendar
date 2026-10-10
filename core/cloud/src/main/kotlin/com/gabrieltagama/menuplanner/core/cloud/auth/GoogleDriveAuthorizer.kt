package com.gabrieltagama.menuplanner.core.cloud.auth

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/**
 * Wraps the Google Identity AuthorizationClient for the Drive app data scope only, the hidden
 * folder that just this app can read. Google handles the account and its password; the app only
 * receives short-lived access tokens. request may need the consent screen, silentToken never
 * shows UI and returns null when the user has to grant access again.
 */
class GoogleDriveAuthorizer @Inject constructor(@param:ApplicationContext private val context: Context) {

    private val client: AuthorizationClient get() = Identity.getAuthorizationClient(context)

    suspend fun request(): AuthorizationStep =
        try {
            client.authorize(authorizationRequest(account = null)).await().toStep()
        } catch (exception: ApiException) {
            AuthorizationStep.Failed
        }

    suspend fun silentToken(email: String): String? =
        try {
            client.authorize(authorizationRequest(accountOf(email))).await().takeUnless { it.hasResolution() }?.accessToken
        } catch (exception: ApiException) {
            null
        }

    fun tokenFromConsent(data: Intent?): String? =
        try {
            client.getAuthorizationResultFromIntent(data).accessToken
        } catch (exception: ApiException) {
            null
        }

    suspend fun invalidate(token: String) = bestEffort {
        client.clearToken(ClearTokenRequest.builder().setToken(token).build()).await()
    }

    suspend fun revoke(email: String) = bestEffort {
        client.revokeAccess(RevokeAccessRequest.builder().setAccount(accountOf(email)).setScopes(listOf(DriveAppDataScope)).build()).await()
    }

    private fun authorizationRequest(account: Account?): AuthorizationRequest {
        val builder = AuthorizationRequest.builder().setRequestedScopes(listOf(DriveAppDataScope))
        if (account != null) builder.setAccount(account)
        return builder.build()
    }

    private suspend fun bestEffort(action: suspend () -> Unit) =
        try {
            action()
        } catch (exception: ApiException) {
            Unit
        }

    private fun accountOf(email: String): Account = Account(email, GOOGLE_ACCOUNT_TYPE)

    private fun AuthorizationResult.toStep(): AuthorizationStep {
        val consent = pendingIntent
        val token = accessToken
        return when {
            hasResolution() && consent != null -> AuthorizationStep.ConsentRequired(consent.intentSender)
            token != null -> AuthorizationStep.Granted(token)
            else -> AuthorizationStep.Failed
        }
    }

    private companion object {
        const val GOOGLE_ACCOUNT_TYPE = "com.google"
        val DriveAppDataScope = Scope("https://www.googleapis.com/auth/drive.appdata")
    }
}
