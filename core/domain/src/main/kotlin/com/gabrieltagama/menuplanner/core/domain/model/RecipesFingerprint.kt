package com.gabrieltagama.menuplanner.core.domain.model

import java.security.MessageDigest

/**
 * Fingerprint of the recipe book built from every dish id and its updatedAt. Adding, editing or
 * deleting a dish changes it, so the automatic backup only uploads when the recipes changed.
 */
object RecipesFingerprint {

    private const val ALGORITHM = "SHA-256"

    fun of(dishes: List<Dish>): String =
        MessageDigest.getInstance(ALGORITHM)
            .digest(dishes.sortedBy(Dish::id).joinToString(separator = "\n", transform = ::versionOf).toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { "%02x".format(it) }

    private fun versionOf(dish: Dish): String = "${dish.id}|${dish.updatedAt.toEpochMilli()}"
}
