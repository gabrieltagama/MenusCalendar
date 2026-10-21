package com.gabrieltagama.menuplanner.core.data.database.dao

import com.gabrieltagama.menuplanner.core.data.database.entity.DayPlanRow

/**
 * Operations shared by the lunch and dinner day tables, addressed by epoch day. Each Room DAO
 * implements them with the queries of its own table, so RoomDayPlanRepository and the import merge
 * are written once for both meals.
 */
internal interface DayPlanDao<E : DayPlanRow> {
    suspend fun getRange(fromEpochDay: Long, toEpochDay: Long): List<E>
    suspend fun getByDate(epochDay: Long): E?
    suspend fun getAll(): List<E>
    suspend fun upsert(day: E)
    suspend fun upsertAll(days: List<E>)
    suspend fun deleteByDate(epochDay: Long)
    suspend fun deleteByDates(epochDays: List<Long>)
}
