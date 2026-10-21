package com.gabrieltagama.menuplanner.core.data.repository

import com.gabrieltagama.menuplanner.core.data.database.dao.DinnerDayDao
import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DinnerDayEntity
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.port.DinnerPlanRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Room-backed dinner calendar (write side and the reads that commands need). Dishes are resolved
 * with one query and several dinners are saved in one transaction. The calendar screen reads
 * through RoomCalendarQueries instead.
 */
internal class RoomDinnerPlanRepository @Inject constructor(
    private val dinnerDayDao: DinnerDayDao,
    private val dishDao: DishDao
) : DinnerPlanRepository {

    override suspend fun getDay(date: LocalDate): DinnerDay? =
        dinnerDayDao.getByDate(date.toEpochDay())?.let { resolve(listOf(it)).firstOrNull() }

    override suspend fun getRange(dates: ClosedRange<LocalDate>): List<DinnerDay> =
        resolve(dinnerDayDao.getRange(dates.start.toEpochDay(), dates.endInclusive.toEpochDay()))

    override suspend fun save(day: DinnerDay) = dinnerDayDao.upsert(day.toEntity())

    override suspend fun saveAll(days: List<DinnerDay>) = dinnerDayDao.upsertAll(days.map { it.toEntity() })

    override suspend fun clear(date: LocalDate) = dinnerDayDao.deleteByDate(date.toEpochDay())

    override suspend fun clearAll(dates: Collection<LocalDate>) = dinnerDayDao.deleteByDates(dates.map(LocalDate::toEpochDay))

    private suspend fun resolve(days: List<DinnerDayEntity>): List<DinnerDay> {
        val dishesById = dishDao.dishesById(days.map { it.dishId }.distinct())
        return days.mapNotNull { it.toDomain(dishesById) }
    }
}
