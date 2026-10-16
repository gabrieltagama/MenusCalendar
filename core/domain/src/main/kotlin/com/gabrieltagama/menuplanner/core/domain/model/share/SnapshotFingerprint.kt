package com.gabrieltagama.menuplanner.core.domain.model.share

import java.security.MessageDigest

/**
 * Fingerprint of a snapshot built from the identity and last change of every dish, lunch and
 * dinner. Adding, editing or deleting any of them changes it, so the automatic backup only uploads
 * when something changed.
 */
object SnapshotFingerprint {

    private const val ALGORITHM = "SHA-256"

    fun of(snapshot: MenuSnapshot): String =
        MessageDigest.getInstance(ALGORITHM)
            .digest(versionsOf(snapshot).joinToString(separator = "\n").toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { "%02x".format(it) }

    private fun versionsOf(snapshot: MenuSnapshot): List<String> =
        snapshot.dishes.map { "dish|${it.id.value}|${it.updatedAt.toEpochMilli()}" }.sorted() +
            snapshot.lunches.map { "lunch|${it.date}|${it.starterId}|${it.mainId}|${it.singleId}|${it.updatedAt.toEpochMilli()}" }.sorted() +
            snapshot.dinners.map { "dinner|${it.date}|${it.dishId}|${it.updatedAt.toEpochMilli()}" }.sorted()
}
