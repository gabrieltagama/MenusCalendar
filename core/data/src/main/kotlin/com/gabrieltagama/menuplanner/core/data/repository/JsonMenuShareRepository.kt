package com.gabrieltagama.menuplanner.core.data.repository

import androidx.room.withTransaction
import com.gabrieltagama.menuplanner.core.data.database.MenuPlannerDatabase
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.share.ShareDocumentCodec
import com.gabrieltagama.menuplanner.core.data.share.ShareDocumentMapper
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.ImportContent
import com.gabrieltagama.menuplanner.core.domain.port.MenuShareRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * JSON sharing of the recipe book and calendar (lunches and dinners). Export reads a consistent
 * snapshot and the recipes export leaves the calendar out; decode validates a received file and
 * returns its content, which ImportMenusUseCase merges.
 */
internal class JsonMenuShareRepository @Inject constructor(
    private val database: MenuPlannerDatabase,
    private val codec: ShareDocumentCodec,
    private val clock: Clock
) : MenuShareRepository {

    override suspend fun exportAll(): String {
        val document = database.withTransaction {
            ShareDocumentMapper.toDocument(
                dishes = database.dishDao().getAll().map(DishWithIngredients::toDomain),
                mealDays = database.mealDayDao().getAll(),
                exportedAt = Instant.now(clock),
                dinnerDays = database.dinnerDayDao().getAll()
            )
        }
        return codec.encode(document)
    }

    override suspend fun exportRecipes(): String = codec.encode(
        ShareDocumentMapper.toDocument(
            dishes = database.dishDao().getAll().map(DishWithIngredients::toDomain),
            mealDays = emptyList(),
            exportedAt = Instant.now(clock)
        )
    )

    override suspend fun decode(json: String): Outcome<ImportContent> =
        when (val decoded = codec.decode(json)) {
            is Outcome.Failure -> decoded
            is Outcome.Success -> ShareDocumentMapper.toImportContent(decoded.value)
        }
}
