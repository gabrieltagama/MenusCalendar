package com.gabrieltagama.menuplanner.core.domain.common

import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import java.time.LocalDate

/**
 * Business errors, each nested in the context that raises it: the recipe book (RecipeError), the
 * meal planning (PlanningError), the shared files (ShareError), the cloud backup (CloudError) and
 * the device storage that could not read or write the data (StorageError).
 * Every error is still a DomainError, so screens can show any of them, while code that only deals
 * with one context can use its narrower type.
 */
sealed interface DomainError

sealed interface RecipeError : DomainError {
    data object BlankDishName : RecipeError
    data class InvalidIngredient(val index: Int) : RecipeError
    data object DishNotFound : RecipeError
    data object DishInUse : RecipeError
    data class DishPlannedIncompatibly(val firstDate: LocalDate, val days: Int) : RecipeError
}

sealed interface PlanningError : DomainError {
    data class WrongDishType(val slot: MenuSlot, val expected: DishType) : PlanningError
    data object NotADinnerDish : PlanningError
}

sealed interface ShareError : DomainError {
    data object InvalidImportFile : ShareError
    data object UnsupportedImportVersion : ShareError
}

sealed interface CloudError : DomainError {
    data object CloudUnavailable : CloudError
    data object CloudAuthorizationRequired : CloudError
}

sealed interface StorageError : DomainError {
    data object StorageUnavailable : StorageError
}
