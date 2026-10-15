package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.port.TransactionRunner

/**
 * TransactionRunner that runs the block directly and counts how many transactions were opened.
 */
class FakeTransactionRunner : TransactionRunner {
    var transactions = 0
        private set

    override suspend fun <R> run(block: suspend () -> R): R {
        transactions++
        return block()
    }
}
