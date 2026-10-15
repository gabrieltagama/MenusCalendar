package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.measure.IngredientStandardizer
import com.gabrieltagama.menuplanner.core.domain.repository.DishRepository
import javax.inject.Inject

/**
 * Leaves the whole recipe book in standard units (see IngredientStandardizer), so a product has
 * the same unit in every dish. Runs on app start, after imports and when a dish is saved; save
 * also stores the edited dish already converted. Other dishes are written only when they change,
 * keeping their updatedAt because the user did not edit them. All writes of a run go in one
 * atomic batch.
 */
class StandardizeRecipeBookUseCase @Inject constructor(private val repository: DishRepository) {

    suspend operator fun invoke() {
        val stored = storedDishes()
        val changed = changedDishes(stored, IngredientStandardizer.standardize(stored))
        if (changed.isNotEmpty()) repository.upsertAll(changed)
    }

    suspend fun save(edited: Dish): Dish {
        val stored = storedDishes(excludedId = edited.id)
        val standard = IngredientStandardizer.standardize(stored + edited)
        val saved = standard.last()
        repository.upsertAll(listOf(saved) + changedDishes(stored, standard))
        return saved
    }

    private suspend fun storedDishes(excludedId: String? = null): List<Dish> =
        repository.getDishes().filterNot { it.id == excludedId }

    private fun changedDishes(stored: List<Dish>, standard: List<Dish>): List<Dish> =
        standard.zip(stored)
            .filter { (converted, original) -> converted != original }
            .map { (converted, _) -> converted }
}
