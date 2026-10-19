package com.gabrieltagama.menuplanner.core.domain.common

import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import java.time.LocalDate

/**
 * Business errors grouped by the context that raises them: the recipe book, the meal planning, the
 * shared files and the cloud backup. Every error is still a DomainError, so screens can show any of
 * them, while code that only deals with one context can use its narrower type.
 */
sealed interface DomainError {
    data object BlankDishName : RecipeError
    data class InvalidIngredient(val index: Int) : RecipeError
    data object DishNotFound : RecipeError
    data object DishInUse : RecipeError
    data class DishPlannedIncompatibly(val firstDate: LocalDate, val days: Int) : RecipeError
    data class WrongDishType(val slot: MenuSlot, val expected: DishType) : PlanningError
    data object NotADinnerDish : PlanningError
    data object InvalidImportFile : ShareError
    data object UnsupportedImportVersion : ShareError
    data object CloudUnavailable : CloudError
    data object CloudAuthorizationRequired : CloudError
}

sealed interface RecipeError : DomainError

sealed interface PlanningError : DomainError

sealed interface ShareError : DomainError

sealed interface CloudError : DomainError
