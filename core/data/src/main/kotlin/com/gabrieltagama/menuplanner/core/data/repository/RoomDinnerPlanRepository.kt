package com.gabrieltagama.menuplanner.core.data.repository

import com.gabrieltagama.menuplanner.core.data.database.dao.DinnerDayDao
import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DinnerDayEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.repository.DinnerPlanRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Room-backed monthly dinner calendar. Like the lunch calendar, the month flow is combined with
 * the dish versions flow so an edited dish refreshes it; dishes are resolved with one query.
 */
internal class RoomDinnerPlanRepository @Inject constructor(
    private val dinnerDayDao: DinnerDayDao,
    private val dishDao: DishDao
) : DinnerPlanRepository {

    override fun observeMonth(month: YearMonth): Flow<List<DinnerDay>> =
        combine(
            dinnerDayDao.observeRange(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay()),
            dishDao.observeVersions()
        ) { days, _ -> resolve(days) }

    override suspend fun getDay(date: LocalDate): DinnerDay? =
        dinnerDayDao.getByDate(date.toEpochDay())?.let { resolve(listOf(it)).firstOrNull() }

    override suspend fun save(day: DinnerDay) = dinnerDayDao.upsert(day.toEntity())

    override suspend fun clear(date: LocalDate) = dinnerDayDao.deleteByDate(date.toEpochDay())

    private suspend fun resolve(days: List<DinnerDayEntity>): List<DinnerDay> {
        val dishesById = loadDishes(days.map { it.dishId }.distinct())
        return days.mapNotNull { it.toDomain(dishesById) }
    }

    private suspend fun loadDishes(ids: List<String>): Map<String, Dish> =
        if (ids.isEmpty()) emptyMap()
        else dishDao.getByIds(ids).map(DishWithIngredients::toDomain).associateBy(Dish::id)
}
