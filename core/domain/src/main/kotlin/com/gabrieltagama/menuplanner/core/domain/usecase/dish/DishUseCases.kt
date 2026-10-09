package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.repository.DishRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use cases of the recipe book: query, save (with validation) and delete (blocked while the
 * dish is planned in the calendar).
 */
class ObserveDishesUseCase @Inject constructor(private val repository: DishRepository) {
    operator fun invoke(type: DishType? = null): Flow<List<Dish>> =
        type?.let(repository::observeDishesByType) ?: repository.observeDishes()
}

class GetDishUseCase @Inject constructor(private val repository: DishRepository) {
    suspend operator fun invoke(id: String): Outcome<Dish> =
        repository.getDish(id)?.let { Outcome.Success(it) } ?: Outcome.Failure(DomainError.DishNotFound)
}

class SaveDishUseCase @Inject constructor(
    private val repository: DishRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(dish: Dish): Outcome<Dish> {
        validate(dish)?.let { return Outcome.Failure(it) }
        val normalized = dish.copy(
            name = dish.name.trim(),
            description = dish.description.trim(),
            preparation = dish.preparation.trim(),
            ingredients = dish.ingredients.map { it.copy(name = it.name.trim()) },
            updatedAt = Instant.now(clock)
        )
        repository.upsert(normalized)
        return Outcome.Success(normalized)
    }

    private fun validate(dish: Dish): DomainError? {
        if (dish.name.isBlank()) return DomainError.BlankDishName
        val invalidIndex = dish.ingredients.indexOfFirst { it.name.isBlank() || !it.quantity.isFinite() || it.quantity < 0.0 }
        return if (invalidIndex >= 0) DomainError.InvalidIngredient(invalidIndex) else null
    }
}

class DeleteDishUseCase @Inject constructor(private val repository: DishRepository) {
    suspend operator fun invoke(id: String): Outcome<Unit> {
        if (repository.getDish(id) == null) return Outcome.Failure(DomainError.DishNotFound)
        if (repository.isUsedInCalendar(id)) return Outcome.Failure(DomainError.DishInUse)
        repository.delete(id)
        return Outcome.Success(Unit)
    }
}
