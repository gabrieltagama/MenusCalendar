package com.gabrieltagama.menuplanner.feature.dishes.testing

import app.cash.turbine.ReceiveTurbine

/**
 * Skips StateFlow emissions until one matches, so tests do not depend on how many intermediate
 * states the unconfined dispatcher conflates.
 */
suspend fun <T> ReceiveTurbine<T>.awaitItemMatching(predicate: (T) -> Boolean): T {
    while (true) {
        val item = awaitItem()
        if (predicate(item)) return item
    }
}
