package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.model.DishId

/**
 * Port that gives the identity of a new dish, so creating one has no hidden randomness and tests
 * can use fixed ids. Implemented in :core:data with random UUIDs.
 */
fun interface DishIdGenerator {
    fun newId(): DishId
}
