package com.gabrieltagama.menuplanner.core.data.repository

import androidx.room.withTransaction
import com.gabrieltagama.menuplanner.core.data.database.MenuPlannerDatabase
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.share.ShareDocumentCodec
import com.gabrieltagama.menuplanner.core.data.share.ShareDocumentMapper
import com.gabrieltagama.menuplanner.core.data.share.ShareMerger
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.repository.MenuShareRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * JSON sharing of the recipe book and calendar. Export reads a consistent snapshot; import is
 * decoded and validated first and then merged inside a single transaction.
 */
internal class JsonMenuShareRepository @Inject constructor(
    private val database: MenuPlannerDatabase,
    private val codec: ShareDocumentCodec,
    private val merger: ShareMerger,
    private val clock: Clock
) : MenuShareRepository {

    override suspend fun exportAll(): String {
        val document = database.withTransaction {
            ShareDocumentMapper.toDocument(
                dishes = database.dishDao().getAll().map(DishWithIngredients::toDomain),
                mealDays = database.mealDayDao().getAll(),
                exportedAt = Instant.now(clock)
            )
        }
        return codec.encode(document)
    }

    override suspend fun importAndMerge(json: String): Outcome<ImportSummary> =
        when (val decoded = codec.decode(json)) {
            is Outcome.Failure -> decoded
            is Outcome.Success -> when (val content = ShareDocumentMapper.toImportContent(decoded.value)) {
                is Outcome.Failure -> content
                is Outcome.Success -> Outcome.Success(database.withTransaction { merger.merge(content.value) })
            }
        }
}
