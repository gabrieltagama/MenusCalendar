package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.measure.IngredientStandardizer
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import javax.inject.Inject

/**
 * Leaves the whole recipe book in standard units (see IngredientStandardizer), so a product has
 * the same unit in every dish. Runs after imports, when a dish is saved and once per conversion
 * version on app start (MigrateRecipeUnitsUseCase); save also stores the edited dish already
 * converted. Other dishes are written only when they change, keeping their updatedAt because the
 * user did not edit them, and every write of one run goes in a single transaction.
 */
class StandardizeRecipeBookUseCase @Inject constructor(private val repository: DishRepository) {

    suspend operator fun invoke() {
        val stored = repository.getDishes()
        val changed = changedDishes(stored, standardizedById(stored))
        if (changed.isNotEmpty()) repository.upsertAll(changed)
    }

    suspend fun save(edited: Dish): Dish {
        val stored = repository.getDishes().filterNot { it.id == edited.id }
        val standard = standardizedById(stored + edited)
        val saved = standard.getValue(edited.id)
        repository.upsertAll(changedDishes(stored, standard) + saved)
        return saved
    }

    private fun standardizedById(dishes: List<Dish>): Map<DishId, Dish> = IngredientStandardizer.standardize(dishes).associateBy(Dish::id)

    private fun changedDishes(stored: List<Dish>, standard: Map<DishId, Dish>): List<Dish> =
        stored.mapNotNull { original -> standard.getValue(original.id).takeIf { it != original } }
}
