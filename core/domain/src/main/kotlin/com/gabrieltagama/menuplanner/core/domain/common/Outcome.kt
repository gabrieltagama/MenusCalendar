package com.gabrieltagama.menuplanner.core.domain.common

/**
 * Explicit success/failure result returned by use cases, so business errors are values
 * instead of exceptions. map and flatMap chain steps that only run after a success, fold turns
 * both cases into one value, onSuccess and onFailure run a side action for one case, and
 * getOrNull drops the error when only the value matters.
 */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: DomainError) : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

inline fun <T, R> Outcome<T>.flatMap(transform: (T) -> Outcome<R>): Outcome<R> = when (this) {
    is Outcome.Success -> transform(value)
    is Outcome.Failure -> this
}

inline fun <T, R> Outcome<T>.fold(onSuccess: (T) -> R, onFailure: (DomainError) -> R): R = when (this) {
    is Outcome.Success -> onSuccess(value)
    is Outcome.Failure -> onFailure(error)
}

inline fun <T> Outcome<T>.onSuccess(action: (T) -> Unit): Outcome<T> {
    if (this is Outcome.Success) action(value)
    return this
}

inline fun <T> Outcome<T>.onFailure(action: (DomainError) -> Unit): Outcome<T> {
    if (this is Outcome.Failure) action(error)
    return this
}

fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Success)?.value
