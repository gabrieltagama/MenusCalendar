package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import com.gabrieltagama.menuplanner.core.domain.port.MenuSnapshotCodec
import java.time.Instant

/**
 * In-memory MenuSnapshotCodec: encode keeps the snapshot and returns a text naming how many dishes,
 * lunches and dinners it has; decode returns the snapshot registered for a text, or failure.
 */
class FakeMenuSnapshotCodec(private val failure: Outcome.Failure? = null) : MenuSnapshotCodec {
    private val snapshotsByText = mutableMapOf<String, MenuSnapshot>()
    val encoded = mutableListOf<MenuSnapshot>()

    fun register(text: String, snapshot: MenuSnapshot) {
        snapshotsByText[text] = snapshot
    }

    override fun encode(snapshot: MenuSnapshot, exportedAt: Instant): String {
        encoded += snapshot
        return textOf(snapshot).also { register(it, snapshot) }
    }

    override fun decode(text: String): Outcome<MenuSnapshot> =
        failure ?: snapshotsByText[text]?.let { Outcome.Success(it) } ?: Outcome.Failure(DomainError.InvalidImportFile)

    companion object {
        fun textOf(snapshot: MenuSnapshot): String = "dishes=${snapshot.dishes.size};lunches=${snapshot.lunches.size};dinners=${snapshot.dinners.size}"
    }
}
