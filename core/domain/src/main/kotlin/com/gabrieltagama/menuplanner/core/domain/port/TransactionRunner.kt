package com.gabrieltagama.menuplanner.core.domain.port

/**
 * Port (unit of work) that runs several repository calls as one transaction: either all of their
 * writes are kept or none is. Implemented in :core:data with a Room transaction.
 */
interface TransactionRunner {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}
