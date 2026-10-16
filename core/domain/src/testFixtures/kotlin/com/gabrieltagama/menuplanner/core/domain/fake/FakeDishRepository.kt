package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.repository.DishRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory DishRepository backed by a MutableStateFlow; usedInCalendar marks ids planned in the calendar.
 */
class FakeDishRepository(initial: List<Dish> = emptyList()) : DishRepository {
    private val dishes = MutableStateFlow(initial.associateBy { it.id })
    val usedInCalendar = mutableSetOf<String>()
    val upserted = mutableListOf<Dish>()
    val deletedIds = mutableListOf<String>()

    val current: Map<String, Dish> get() = dishes.value

    override fun observeDishes(): Flow<List<Dish>> = dishes.map { it.values.toList() }

    override fun observeDishesByType(type: DishType): Flow<List<Dish>> =
        dishes.map { all -> all.values.filter { it.type == type } }

    override suspend fun getDishes(): List<Dish> = dishes.value.values.toList()

    override suspend fun getDish(id: String): Dish? = dishes.value[id]

    override suspend fun upsert(dish: Dish) = upsertAll(listOf(dish))

    override suspend fun upsertAll(dishes: List<Dish>) {
        upserted += dishes
        this.dishes.value = this.dishes.value + dishes.associateBy { it.id }
    }

    override suspend fun deleteIfUnused(id: String): Boolean {
        if (id in usedInCalendar) return false
        deletedIds += id
        dishes.value = dishes.value - id
        return true
    }
}
