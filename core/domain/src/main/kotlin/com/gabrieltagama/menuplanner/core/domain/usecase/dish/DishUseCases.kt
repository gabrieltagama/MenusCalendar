package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.RecipeError
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishDraft
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.PlannedUse
import com.gabrieltagama.menuplanner.core.domain.port.DishIdGenerator
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import com.gabrieltagama.menuplanner.core.domain.port.TransactionRunner
import com.gabrieltagama.menuplanner.core.domain.query.PlannedUseQueries
import java.time.Clock
import javax.inject.Inject

/**
 * Commands of the recipe book (read through DishCatalogQueries): get a dish, save a draft (validated with the Dish rules, given an id
 * when new and converted to standard units) and delete (blocked while the dish is planned). Saving
 * a planned dish with a type or meal time that its calendar days no longer accept is refused with
 * DishPlannedIncompatibly, so the calendar never holds days that an export could not restore; the
 * check and the save run in one transaction.
 */
class GetDishUseCase @Inject constructor(private val repository: DishRepository) {
    suspend operator fun invoke(id: DishId): Outcome<Dish> =
        repository.getDish(id)?.let { Outcome.Success(it) } ?: Outcome.Failure(RecipeError.DishNotFound)
}

class SaveDishUseCase @Inject constructor(
    private val plannedUses: PlannedUseQueries,
    private val clock: Clock,
    private val idGenerator: DishIdGenerator,
    private val standardizeRecipeBook: StandardizeRecipeBookUseCase,
    private val transaction: TransactionRunner
) {
    suspend operator fun invoke(draft: DishDraft): Outcome<Dish> {
        draft.validationError?.let { return Outcome.Failure(it) }
        return transaction.inTransaction {
            val conflict = draft.id?.let { planConflictOf(it, draft) }
            if (conflict != null) Outcome.Failure(conflict)
            else Outcome.Success(standardizeRecipeBook.save(draft.toDish(id = draft.id ?: idGenerator.newId(), updatedAt = clock.instant())))
        }
    }

    private suspend fun planConflictOf(id: DishId, draft: DishDraft): DomainError? =
        plannedUses.getPlannedUses(id)
            .filterNot { it.allows(draft.type, draft.mealTime) }
            .takeIf { it.isNotEmpty() }
            ?.let { conflicts -> RecipeError.DishPlannedIncompatibly(firstDate = conflicts.minOf(PlannedUse::date), days = conflicts.map(PlannedUse::date).distinct().size) }
}

class DeleteDishUseCase @Inject constructor(private val repository: DishRepository) {
    suspend operator fun invoke(id: DishId): Outcome<Unit> {
        if (repository.getDish(id) == null) return Outcome.Failure(RecipeError.DishNotFound)
        return if (repository.deleteIfUnused(id)) Outcome.Success(Unit) else Outcome.Failure(RecipeError.DishInUse)
    }
}
