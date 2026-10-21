package com.gabrieltagama.menuplanner.core.data.database.dao

import com.gabrieltagama.menuplanner.core.data.database.entity.DayPlanRow
import java.time.LocalDate

/**
 * Operations shared by the lunch and dinner day tables, addressed by date. Each Room DAO
 * implements them with the queries of its own table, so RoomDayPlanRepository and the import merge
 * are written once for both meals.
 */
internal interface DayPlanDao<E : DayPlanRow> {
    suspend fun getRange(from: LocalDate, to: LocalDate): List<E>
    suspend fun getByDate(date: LocalDate): E?
    suspend fun getAll(): List<E>
    suspend fun upsert(day: E)
    suspend fun upsertAll(days: List<E>)
    suspend fun deleteByDate(date: LocalDate)
    suspend fun deleteByDates(dates: List<LocalDate>)
}
