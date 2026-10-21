package com.gabrieltagama.menuplanner.core.domain.common

/**
 * Explicit success/failure result returned by use cases, so business errors are values
 * instead of exceptions. onSuccess runs an action only with a successful value and getOrNull
 * drops the error when only the value matters.
 */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: DomainError) : Outcome<Nothing>
}

inline fun <T> Outcome<T>.onSuccess(action: (T) -> Unit): Outcome<T> {
    if (this is Outcome.Success) action(value)
    return this
}

fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Success)?.value
