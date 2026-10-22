package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.measure.IngredientStandardizer
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import javax.inject.Inject

/**
 * Policy of the recipe book units (see IngredientStandardizer): a product has the same standard unit
 * in every dish. invoke aligns the whole book and runs after imports, after a dish is saved and once
 * per conversion version on app start (MigrateRecipeUnitsUseCase); other dishes are written only
 * when they change, keeping their updatedAt because the user did not edit them, all in one write.
 * save writes only the edited dish, already in the units the rest of the book uses; aligning the
 * rest with it is the separate step that follows.
 */
class StandardizeRecipeBookUseCase @Inject constructor(private val repository: DishRepository) {

    suspend operator fun invoke() {
        val stored = repository.getDishes()
        val standard = standardizedById(stored)
        val changed = stored.mapNotNull { original -> standard.getValue(original.id).takeIf { it != original } }
        if (changed.isNotEmpty()) repository.upsertAll(changed)
    }

    suspend fun save(edited: Dish): Dish {
        val others = repository.getDishes().filterNot { it.id == edited.id }
        val saved = standardizedById(others + edited).getValue(edited.id)
        repository.upsert(saved)
        return saved
    }

    private fun standardizedById(dishes: List<Dish>): Map<DishId, Dish> = IngredientStandardizer.standardize(dishes).associateBy(Dish::id)
}
