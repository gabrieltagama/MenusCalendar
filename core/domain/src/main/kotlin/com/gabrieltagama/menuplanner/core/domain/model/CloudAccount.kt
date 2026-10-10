package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Google account linked to the personal recipe backup. Disconnected means the app works only
 * with the data stored on the device.
 */
sealed interface CloudAccount {
    data object Disconnected : CloudAccount
    data class Connected(val email: String) : CloudAccount
}
