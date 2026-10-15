package com.gabrieltagama.menuplanner.core.data.fake

import com.gabrieltagama.menuplanner.core.data.database.dao.DinnerDayDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DinnerDayEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory DinnerDayDao for JVM tests, keyed by epoch day.
 */
internal class FakeDinnerDayDao : DinnerDayDao {
    private val days = MutableStateFlow<Map<Long, DinnerDayEntity>>(emptyMap())

    val currentDays: Map<Long, DinnerDayEntity> get() = days.value

    override fun observeRange(fromEpochDay: Long, toEpochDay: Long): Flow<List<DinnerDayEntity>> =
        days.map { all -> all.values.filter { it.date in fromEpochDay..toEpochDay }.sortedBy(DinnerDayEntity::date) }

    override suspend fun getByDate(epochDay: Long): DinnerDayEntity? = days.value[epochDay]

    override suspend fun getAll(): List<DinnerDayEntity> = days.value.values.sortedBy(DinnerDayEntity::date)

    override suspend fun upsert(day: DinnerDayEntity) {
        days.value = days.value + (day.date to day)
    }

    override suspend fun deleteByDate(epochDay: Long) {
        days.value = days.value - epochDay
    }
}
