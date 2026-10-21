package com.gabrieltagama.menuplanner.core.data.id

import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.port.DishIdGenerator
import java.util.UUID
import javax.inject.Inject

/**
 * Production DishIdGenerator: every new dish gets a random UUID, so dishes created on different
 * devices never clash when a shared JSON is imported.
 */
internal class UuidDishIdGenerator @Inject constructor() : DishIdGenerator {
    override fun newId(): DishId = DishId(UUID.randomUUID().toString())
}
