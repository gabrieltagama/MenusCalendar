package com.gabrieltagama.menuplanner.core.data.repository

import com.gabrieltagama.menuplanner.core.data.database.dao.DayPlanDao
import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DayPlanRow
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.PlannedDay
import com.gabrieltagama.menuplanner.core.domain.port.DayPlanRepository
import java.time.LocalDate

/**
 * Room-backed calendar of one meal (write side and the reads that commands need), written once for
 * lunches and dinners: each meal gives its DAO and how its rows map to the domain. Referenced dishes
 * are resolved with one query per read, a row with an unknown dish is skipped, and several days are
 * saved in one transaction. The calendar screen reads through RoomCalendarQueries instead.
 */
internal abstract class RoomDayPlanRepository<E : DayPlanRow, D : PlannedDay<D>>(
    private val dao: DayPlanDao<E>,
    private val dishDao: DishDao,
    private val dishIdsOf: (E) -> List<String>,
    private val toDomain: (E, Map<DishId, Dish>) -> D?,
    private val toEntity: (D) -> E
) : DayPlanRepository<D> {

    override suspend fun getDay(date: LocalDate): D? = dao.getByDate(date)?.let { resolve(listOf(it)).firstOrNull() }

    override suspend fun getRange(dates: ClosedRange<LocalDate>): List<D> =
        resolve(dao.getRange(dates.start, dates.endInclusive))

    override suspend fun save(day: D) = dao.upsert(toEntity(day))

    override suspend fun saveAll(days: List<D>) = dao.upsertAll(days.map(toEntity))

    override suspend fun clear(date: LocalDate) = dao.deleteByDate(date)

    override suspend fun clearAll(dates: Collection<LocalDate>) = dao.deleteByDates(dates.toList())

    private suspend fun resolve(rows: List<E>): List<D> {
        val dishesById = dishDao.dishesById(rows.flatMap(dishIdsOf).distinct())
        return rows.mapNotNull { toDomain(it, dishesById) }
    }
}
