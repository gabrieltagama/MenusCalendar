package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.PlannedUse
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory DishRepository backed by a MutableStateFlow; usedInCalendar marks ids planned in the
 * calendar and plannedUses gives the calendar days returned by getPlannedUses. Ids are exposed as plain text (current, usedInCalendar, deletedIds) to keep tests short.
 */
class FakeDishRepository(initial: List<Dish> = emptyList()) : DishRepository {
    private val dishes = MutableStateFlow(initial.associateBy { it.id })
    val usedInCalendar = mutableSetOf<String>()
    val upserted = mutableListOf<Dish>()
    val deletedIds = mutableListOf<String>()
    val plannedUses = mutableMapOf<String, List<PlannedUse>>()

    val current: Map<String, Dish> get() = dishes.value.mapKeys { it.key.value }

    override fun observeDishes(): Flow<List<Dish>> = dishes.map { it.values.toList() }

    override suspend fun getDishes(): List<Dish> = dishes.value.values.toList()

    override suspend fun getDish(id: DishId): Dish? = dishes.value[id]

    override suspend fun upsert(dish: Dish) = upsertAll(listOf(dish))

    override suspend fun upsertAll(dishes: List<Dish>) {
        upserted += dishes
        this.dishes.value = this.dishes.value + dishes.associateBy { it.id }
    }

    override suspend fun deleteIfUnused(id: DishId): Boolean {
        if (id.value in usedInCalendar) return false
        deletedIds += id.value
        dishes.value = dishes.value - id
        return true
    }

    override suspend fun getPlannedUses(id: DishId): List<PlannedUse> = plannedUses[id.value].orEmpty()
}
