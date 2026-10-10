package com.gabrieltagama.menuplanner.core.domain.common

import com.gabrieltagama.menuplanner.core.domain.model.DishType

/**
 * Explicit success/failure result returned by use cases, so business errors are values
 * instead of exceptions.
 */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: DomainError) : Outcome<Nothing>
}

sealed interface DomainError {
    data object BlankDishName : DomainError
    data class InvalidIngredient(val index: Int) : DomainError
    data object DishNotFound : DomainError
    data object DishInUse : DomainError
    data class WrongDishType(val slot: MenuSlot, val expected: DishType) : DomainError
    data object InvalidImportFile : DomainError
    data object UnsupportedImportVersion : DomainError
    data object CloudUnavailable : DomainError
    data object CloudAuthorizationRequired : DomainError
}

enum class MenuSlot { STARTER, MAIN, SINGLE, DESSERT }
