package com.gabrieltagama.menuplanner.core.data.fake

import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DishEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.DishVersion
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.database.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory DishDao for JVM tests. Usage counting is delegated to an optional FakeMealDayDao;
 * upsertWithIngredients uses the interface default implementation.
 */
internal class FakeDishDao(private val mealDayDao: FakeMealDayDao? = null) : DishDao {
    private val dishes = MutableStateFlow<Map<String, DishEntity>>(emptyMap())
    private val ingredients = MutableStateFlow<List<IngredientEntity>>(emptyList())
    private var nextIngredientId = 1L

    val upsertedIds = mutableListOf<String>()

    val currentDishes: Map<String, DishEntity> get() = dishes.value

    override fun observeAll(): Flow<List<DishWithIngredients>> = dishes.map { sortedWithIngredients(it.values) }

    override fun observeByType(type: String): Flow<List<DishWithIngredients>> =
        dishes.map { all -> sortedWithIngredients(all.values.filter { it.type == type }) }

    override fun observeVersions(): Flow<List<DishVersion>> = dishes.map { all -> all.values.map(::versionOf) }

    override suspend fun getById(id: String): DishWithIngredients? = dishes.value[id]?.let(::withIngredients)

    override suspend fun getByIds(ids: List<String>): List<DishWithIngredients> =
        ids.mapNotNull { dishes.value[it] }.map(::withIngredients)

    override suspend fun getAll(): List<DishWithIngredients> = sortedWithIngredients(dishes.value.values)

    override suspend fun getVersions(): List<DishVersion> = dishes.value.values.map(::versionOf)

    override suspend fun countUsages(id: String): Int = mealDayDao?.countUsages(id) ?: 0

    override suspend fun upsertDish(dish: DishEntity) {
        upsertedIds += dish.id
        dishes.value = dishes.value + (dish.id to dish)
    }

    override suspend fun insertIngredients(ingredients: List<IngredientEntity>) {
        this.ingredients.value = this.ingredients.value + ingredients.map { it.copy(id = nextIngredientId++) }
    }

    override suspend fun deleteIngredients(dishId: String) {
        ingredients.value = ingredients.value.filterNot { it.dishId == dishId }
    }

    override suspend fun deleteById(id: String) {
        dishes.value = dishes.value - id
        deleteIngredients(id)
    }

    private fun sortedWithIngredients(entities: Collection<DishEntity>): List<DishWithIngredients> =
        entities.sortedBy { it.name.lowercase() }.map(::withIngredients)

    private fun withIngredients(entity: DishEntity): DishWithIngredients =
        DishWithIngredients(entity, ingredients.value.filter { it.dishId == entity.id })

    private fun versionOf(entity: DishEntity): DishVersion = DishVersion(entity.id, entity.updatedAt)
}
