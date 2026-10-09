package com.gabrieltagama.menuplanner.feature.calendar.testing

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.repository.DishRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory DishRepository backed by a MutableStateFlow, used by the autofill tests of the calendar.
 */
class FakeDishRepository(initial: List<Dish> = emptyList()) : DishRepository {
    private val dishes = MutableStateFlow(initial.associateBy { it.id })

    override fun observeDishes(): Flow<List<Dish>> = dishes.map { it.values.toList() }

    override fun observeDishesByType(type: DishType): Flow<List<Dish>> =
        dishes.map { all -> all.values.filter { it.type == type } }

    override suspend fun getDish(id: String): Dish? = dishes.value[id]

    override suspend fun upsert(dish: Dish) {
        dishes.value = dishes.value + (dish.id to dish)
    }

    override suspend fun delete(id: String) {
        dishes.value = dishes.value - id
    }

    override suspend fun isUsedInCalendar(id: String): Boolean = false
}
