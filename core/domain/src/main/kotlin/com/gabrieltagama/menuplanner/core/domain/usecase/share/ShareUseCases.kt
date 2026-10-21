package com.gabrieltagama.menuplanner.core.domain.usecase.share

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.map
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.share.SnapshotScope
import com.gabrieltagama.menuplanner.core.domain.port.MenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.domain.port.MenuSnapshotRepository
import com.gabrieltagama.menuplanner.core.domain.port.TransactionRunner
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.StandardizeRecipeBookUseCase
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Use cases to share the recipe book and calendar with other users. Export encodes a snapshot of
 * everything; import decodes a received file, merges what the scope covers (everything by default)
 * and converts the recipes to standard units, both in one transaction.
 */
class ExportMenusUseCase @Inject constructor(
    private val repository: MenuSnapshotRepository,
    private val codec: MenuSnapshotCodec,
    private val clock: Clock
) {
    suspend operator fun invoke(): String = codec.encode(repository.read(SnapshotScope.EVERYTHING), Instant.now(clock))
}

class ImportMenusUseCase @Inject constructor(
    private val repository: MenuSnapshotRepository,
    private val codec: MenuSnapshotCodec,
    private val standardizeRecipeBook: StandardizeRecipeBookUseCase,
    private val transaction: TransactionRunner
) {
    suspend operator fun invoke(text: String, scope: SnapshotScope = SnapshotScope.EVERYTHING): Outcome<ImportSummary> =
        codec.decode(text).map { snapshot ->
            transaction.inTransaction { repository.merge(snapshot.limitedTo(scope)).also { standardizeRecipeBook() } }
        }
}
