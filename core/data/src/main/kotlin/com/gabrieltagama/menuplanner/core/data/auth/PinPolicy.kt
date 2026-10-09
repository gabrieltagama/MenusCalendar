package com.gabrieltagama.menuplanner.core.data.auth

import java.security.MessageDigest

/**
 * Local PIN verification. Only the SHA-256 hex digest of the PIN is kept in code and the
 * comparison is constant-time.
 */
internal object PinPolicy {

    private const val PIN_SHA256_HEX = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"
    private const val ALGORITHM = "SHA-256"

    fun matches(pin: String): Boolean =
        MessageDigest.isEqual(sha256Hex(pin).toByteArray(Charsets.US_ASCII), PIN_SHA256_HEX.toByteArray(Charsets.US_ASCII))

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance(ALGORITHM)
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { "%02x".format(it) }
}
