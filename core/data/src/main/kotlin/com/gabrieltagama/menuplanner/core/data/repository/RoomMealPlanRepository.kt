package com.gabrieltagama.menuplanner.core.data.repository

import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.dao.MealDayDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.dishIds
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.repository.MealPlanRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Room-backed monthly calendar. The month flow is combined with the dish versions flow so an
 * edited dish refreshes the calendar; referenced dishes are resolved with one query per emission.
 */
internal class RoomMealPlanRepository @Inject constructor(
    private val mealDayDao: MealDayDao,
    private val dishDao: DishDao
) : MealPlanRepository {

    override fun observeMonth(month: YearMonth): Flow<List<MealDay>> =
        combine(
            mealDayDao.observeRange(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay()),
            dishDao.observeVersions()
        ) { days, _ -> resolve(days) }

    override suspend fun getDay(date: LocalDate): MealDay? =
        mealDayDao.getByDate(date.toEpochDay())?.let { resolve(listOf(it)).firstOrNull() }

    override suspend fun save(day: MealDay) = mealDayDao.upsert(day.toEntity())

    override suspend fun clear(date: LocalDate) = mealDayDao.deleteByDate(date.toEpochDay())

    private suspend fun resolve(days: List<MealDayEntity>): List<MealDay> {
        val dishesById = loadDishes(days.flatMap { it.dishIds }.distinct())
        return days.mapNotNull { it.toDomain(dishesById) }
    }

    private suspend fun loadDishes(ids: List<String>): Map<String, Dish> =
        if (ids.isEmpty()) emptyMap()
        else dishDao.getByIds(ids).map(DishWithIngredients::toDomain).associateBy(Dish::id)
}
