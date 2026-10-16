package com.gabrieltagama.menuplanner.core.data.repository

import com.gabrieltagama.menuplanner.core.data.database.dao.DinnerDayDao
import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DinnerDayEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.repository.DinnerPlanRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Room-backed dinner calendar. Like the lunch calendar, the month flow only refreshes when one of
 * the dishes it references changes; dishes are resolved with one query and several dinners are
 * saved in one transaction.
 */
internal class RoomDinnerPlanRepository @Inject constructor(
    private val dinnerDayDao: DinnerDayDao,
    private val dishDao: DishDao
) : DinnerPlanRepository {

    override fun observeMonth(month: YearMonth): Flow<List<DinnerDay>> =
        combine(
            dinnerDayDao.observeRange(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay()),
            dishDao.observeVersions()
        ) { days, versions ->
            val referenced = days.map { it.dishId }.toSet()
            days to versions.filter { it.id in referenced }.toSet()
        }
            .distinctUntilChanged()
            .map { (days, _) -> resolve(days) }

    override suspend fun getDay(date: LocalDate): DinnerDay? =
        dinnerDayDao.getByDate(date.toEpochDay())?.let { resolve(listOf(it)).firstOrNull() }

    override suspend fun getRange(dates: ClosedRange<LocalDate>): List<DinnerDay> =
        resolve(dinnerDayDao.getRange(dates.start.toEpochDay(), dates.endInclusive.toEpochDay()))

    override suspend fun save(day: DinnerDay) = dinnerDayDao.upsert(day.toEntity())

    override suspend fun saveAll(days: List<DinnerDay>) = dinnerDayDao.upsertAll(days.map { it.toEntity() })

    override suspend fun clear(date: LocalDate) = dinnerDayDao.deleteByDate(date.toEpochDay())

    private suspend fun resolve(days: List<DinnerDayEntity>): List<DinnerDay> {
        val dishesById = loadDishes(days.map { it.dishId }.distinct())
        return days.mapNotNull { it.toDomain(dishesById) }
    }

    private suspend fun loadDishes(ids: List<String>): Map<DishId, Dish> =
        if (ids.isEmpty()) emptyMap()
        else dishDao.getByIds(ids).map(DishWithIngredients::toDomain).associateBy(Dish::id)
}
