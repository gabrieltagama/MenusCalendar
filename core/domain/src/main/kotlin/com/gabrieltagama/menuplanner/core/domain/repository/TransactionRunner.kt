package com.gabrieltagama.menuplanner.core.domain.repository

/**
 * Port that runs a block of repository calls atomically: either every write inside it is kept or
 * none is. Calls may be nested. Implemented in :core:data.
 */
interface TransactionRunner {
    suspend fun <R> run(block: suspend () -> R): R
}
