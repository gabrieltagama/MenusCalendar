package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishDraft
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.repository.DishIdGenerator
import com.gabrieltagama.menuplanner.core.domain.repository.DishRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use cases of the recipe book: query, save a draft (validated with the Dish rules, given an id
 * when new and converted to standard units) and delete (blocked while the dish is planned).
 */
class ObserveDishesUseCase @Inject constructor(private val repository: DishRepository) {
    operator fun invoke(type: DishType? = null): Flow<List<Dish>> =
        type?.let(repository::observeDishesByType) ?: repository.observeDishes()
}

class GetDishUseCase @Inject constructor(private val repository: DishRepository) {
    suspend operator fun invoke(id: DishId): Outcome<Dish> =
        repository.getDish(id)?.let { Outcome.Success(it) } ?: Outcome.Failure(DomainError.DishNotFound)
}

class SaveDishUseCase @Inject constructor(
    private val clock: Clock,
    private val idGenerator: DishIdGenerator,
    private val standardizeRecipeBook: StandardizeRecipeBookUseCase
) {
    suspend operator fun invoke(draft: DishDraft): Outcome<Dish> {
        Dish.validate(draft.name, draft.ingredients)?.let { return Outcome.Failure(it) }
        val dish = Dish(
            id = draft.id ?: idGenerator.newId(),
            name = draft.name.trim(),
            description = draft.description.trim(),
            ingredients = draft.ingredients.map { it.copy(name = it.name.trim()) },
            preparation = draft.preparation.trim(),
            type = draft.type,
            heaviness = draft.heaviness,
            rating = draft.rating,
            mealTime = draft.mealTime,
            updatedAt = Instant.now(clock)
        )
        return Outcome.Success(standardizeRecipeBook.save(dish))
    }
}

class DeleteDishUseCase @Inject constructor(private val repository: DishRepository) {
    suspend operator fun invoke(id: DishId): Outcome<Unit> {
        if (repository.getDish(id) == null) return Outcome.Failure(DomainError.DishNotFound)
        return if (repository.deleteIfUnused(id)) Outcome.Success(Unit) else Outcome.Failure(DomainError.DishInUse)
    }
}
