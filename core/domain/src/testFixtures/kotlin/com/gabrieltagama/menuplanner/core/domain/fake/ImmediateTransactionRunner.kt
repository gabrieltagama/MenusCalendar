package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.port.TransactionRunner

/**
 * TransactionRunner for tests: runs the block directly and counts how many transactions were opened.
 */
class ImmediateTransactionRunner : TransactionRunner {
    var transactions = 0
        private set

    override suspend fun <T> inTransaction(block: suspend () -> T): T {
        transactions++
        return block()
    }
}
