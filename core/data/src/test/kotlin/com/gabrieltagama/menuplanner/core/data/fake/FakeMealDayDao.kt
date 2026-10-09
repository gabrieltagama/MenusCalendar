package com.gabrieltagama.menuplanner.core.data.fake

import com.gabrieltagama.menuplanner.core.data.database.dao.MealDayDao
import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.dishIds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory MealDayDao for JVM tests, keyed by epoch day.
 */
internal class FakeMealDayDao : MealDayDao {
    private val days = MutableStateFlow<Map<Long, MealDayEntity>>(emptyMap())

    val upserted = mutableListOf<MealDayEntity>()

    val currentDays: Map<Long, MealDayEntity> get() = days.value

    override fun observeRange(fromEpochDay: Long, toEpochDay: Long): Flow<List<MealDayEntity>> =
        days.map { all -> all.values.filter { it.date in fromEpochDay..toEpochDay }.sortedBy(MealDayEntity::date) }

    override suspend fun getByDate(epochDay: Long): MealDayEntity? = days.value[epochDay]

    override suspend fun getAll(): List<MealDayEntity> = days.value.values.sortedBy(MealDayEntity::date)

    override suspend fun upsert(day: MealDayEntity) {
        upserted += day
        days.value = days.value + (day.date to day)
    }

    override suspend fun deleteByDate(epochDay: Long) {
        days.value = days.value - epochDay
    }

    fun countUsages(dishId: String): Int = days.value.values.count { dishId in it.dishIds }
}
