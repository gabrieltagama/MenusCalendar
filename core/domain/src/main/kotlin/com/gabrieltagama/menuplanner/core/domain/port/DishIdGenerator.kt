package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.model.DishId
import java.util.UUID
import javax.inject.Inject

/**
 * Port that gives the identity of a new dish, so creating one has no hidden randomness and tests
 * can use fixed ids. UuidDishIdGenerator is the production implementation.
 */
fun interface DishIdGenerator {
    fun newId(): DishId
}

class UuidDishIdGenerator @Inject constructor() : DishIdGenerator {
    override fun newId(): DishId = DishId(UUID.randomUUID().toString())
}
