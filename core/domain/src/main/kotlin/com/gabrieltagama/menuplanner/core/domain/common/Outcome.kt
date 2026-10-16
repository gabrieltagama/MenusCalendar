package com.gabrieltagama.menuplanner.core.domain.common

/**
 * Explicit success/failure result returned by use cases, so business errors are values
 * instead of exceptions.
 */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: DomainError) : Outcome<Nothing>
}
