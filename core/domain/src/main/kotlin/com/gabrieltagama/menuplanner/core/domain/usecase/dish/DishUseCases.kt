package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishDraft
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.PlannedUse
import com.gabrieltagama.menuplanner.core.domain.port.DishIdGenerator
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use cases of the recipe book: query, save a draft (validated with the Dish rules, given an id
 * when new and converted to standard units) and delete (blocked while the dish is planned). Saving
 * a planned dish with a type or meal time that its calendar days no longer accept is refused with
 * DishPlannedIncompatibly, so the calendar never holds days that an export could not restore.
 */
class ObserveDishesUseCase @Inject constructor(private val repository: DishRepository) {
    operator fun invoke(): Flow<List<Dish>> = repository.observeDishes()
}

class GetDishUseCase @Inject constructor(private val repository: DishRepository) {
    suspend operator fun invoke(id: DishId): Outcome<Dish> =
        repository.getDish(id)?.let { Outcome.Success(it) } ?: Outcome.Failure(DomainError.DishNotFound)
}

class SaveDishUseCase @Inject constructor(
    private val repository: DishRepository,
    private val clock: Clock,
    private val idGenerator: DishIdGenerator,
    private val standardizeRecipeBook: StandardizeRecipeBookUseCase
) {
    suspend operator fun invoke(draft: DishDraft): Outcome<Dish> {
        draft.validationError?.let { return Outcome.Failure(it) }
        draft.id?.let { planConflictOf(it, draft) }?.let { return Outcome.Failure(it) }
        val dish = draft.toDish(id = draft.id ?: idGenerator.newId(), updatedAt = Instant.now(clock))
        return Outcome.Success(standardizeRecipeBook.save(dish))
    }

    private suspend fun planConflictOf(id: DishId, draft: DishDraft): DomainError? =
        repository.getPlannedUses(id)
            .filterNot { it.allows(draft.type, draft.mealTime) }
            .takeIf { it.isNotEmpty() }
            ?.let { conflicts -> DomainError.DishPlannedIncompatibly(firstDate = conflicts.minOf(PlannedUse::date), days = conflicts.map(PlannedUse::date).distinct().size) }
}

class DeleteDishUseCase @Inject constructor(private val repository: DishRepository) {
    suspend operator fun invoke(id: DishId): Outcome<Unit> {
        if (repository.getDish(id) == null) return Outcome.Failure(DomainError.DishNotFound)
        return if (repository.deleteIfUnused(id)) Outcome.Success(Unit) else Outcome.Failure(DomainError.DishInUse)
    }
}
