package com.gabrieltagama.menuplanner.core.data.repository

import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.PlannedMeal
import com.gabrieltagama.menuplanner.core.domain.repository.PlanRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Room-backed calendar shared by lunches and dinners. Rows are addressed by epoch day; the month
 * flow is combined with the dish versions flow so an edited dish refreshes it, and the dishes a
 * set of rows references are resolved with one query. Subclasses only provide the row access of
 * their table and the mapping between rows and the domain.
 */
internal abstract class RoomPlanRepository<E, T : PlannedMeal>(private val dishDao: DishDao) : PlanRepository<T> {

    protected abstract fun observeRows(fromEpochDay: Long, toEpochDay: Long): Flow<List<E>>
    protected abstract suspend fun rows(fromEpochDay: Long, toEpochDay: Long): List<E>
    protected abstract suspend fun row(epochDay: Long): E?
    protected abstract suspend fun upsertRows(rows: List<E>)
    protected abstract suspend fun deleteRow(epochDay: Long)
    protected abstract fun dishIdsOf(row: E): List<String>
    protected abstract fun toDomain(row: E, dishesById: Map<String, Dish>): T?
    protected abstract fun toRow(day: T): E

    override fun observeMonth(month: YearMonth): Flow<List<T>> =
        combine(observeRows(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay()), dishDao.observeVersions()) { rows, _ -> resolve(rows) }

    override suspend fun getRange(dates: ClosedRange<LocalDate>): List<T> =
        resolve(rows(dates.start.toEpochDay(), dates.endInclusive.toEpochDay()))

    override suspend fun getDay(date: LocalDate): T? = row(date.toEpochDay())?.let { resolve(listOf(it)).firstOrNull() }

    override suspend fun save(day: T) = upsertRows(listOf(toRow(day)))

    override suspend fun saveAll(days: List<T>) {
        if (days.isNotEmpty()) upsertRows(days.map(::toRow))
    }

    override suspend fun clear(date: LocalDate) = deleteRow(date.toEpochDay())

    private suspend fun resolve(rows: List<E>): List<T> {
        val dishesById = loadDishes(rows.flatMap(::dishIdsOf).distinct())
        return rows.mapNotNull { toDomain(it, dishesById) }
    }

    private suspend fun loadDishes(ids: List<String>): Map<String, Dish> =
        if (ids.isEmpty()) emptyMap()
        else dishDao.getByIds(ids).map(DishWithIngredients::toDomain).associateBy(Dish::id)
}
