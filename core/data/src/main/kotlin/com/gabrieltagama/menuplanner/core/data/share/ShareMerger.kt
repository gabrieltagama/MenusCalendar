package com.gabrieltagama.menuplanner.core.data.share

import com.gabrieltagama.menuplanner.core.data.database.dao.DinnerDayDao
import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.dao.MealDayDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DinnerDayEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.data.mapper.toIngredientEntities
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import javax.inject.Inject

/**
 * Applies MergePolicy to the database. Dishes are merged first so meal days and dinners can
 * reference dishes that arrived in the same file; dinners are counted together with the days.
 * Must be called inside a database transaction.
 */
internal class ShareMerger @Inject constructor(
    private val dishDao: DishDao,
    private val mealDayDao: MealDayDao,
    private val dinnerDayDao: DinnerDayDao
) {

    suspend fun merge(content: ImportContent): ImportSummary {
        val dishes = mergeDishes(content.dishes)
        val days = mergeDays(content.mealDays) + mergeDinners(content.dinnerDays)
        return ImportSummary(
            dishesAdded = dishes.added,
            dishesUpdated = dishes.updated,
            dishesSkipped = dishes.skipped,
            daysAdded = days.added,
            daysUpdated = days.updated,
            daysSkipped = days.skipped
        )
    }

    private suspend fun mergeDishes(dishes: List<Dish>): MergeCount {
        val local = dishDao.getVersions().associate { it.id to it.updatedAt }
        val decisions = dishes.map { it to MergePolicy.decide(local[it.id], it.updatedAt.toEpochMilli()) }
        decisions
            .filter { (_, decision) -> decision != MergeDecision.SKIP }
            .forEach { (dish, _) -> dishDao.upsertWithIngredients(dish.toEntity(), dish.toIngredientEntities()) }
        return MergeCount.of(decisions.map { it.second })
    }

    private suspend fun mergeDays(days: List<MealDayEntity>): MergeCount {
        val knownDishTypes = dishDao.getVersions().associate { it.id to DishType.valueOf(it.type) }
        val local = mealDayDao.getAll().associate { it.date to it.updatedAt }
        val decisions = days.map { it to decideDay(it, local[it.date], knownDishTypes) }
        decisions
            .filter { (_, decision) -> decision != MergeDecision.SKIP }
            .forEach { (day, _) -> mealDayDao.upsert(day) }
        return MergeCount.of(decisions.map { it.second })
    }

    private suspend fun mergeDinners(dinners: List<DinnerDayEntity>): MergeCount {
        val knownDishTypes = dishDao.getVersions().associate { it.id to DishType.valueOf(it.type) }
        val local = dinnerDayDao.getAll().associate { it.date to it.updatedAt }
        val decisions = dinners.map { it to decideDinner(it, local[it.date], knownDishTypes) }
        decisions
            .filter { (_, decision) -> decision != MergeDecision.SKIP }
            .forEach { (dinner, _) -> dinnerDayDao.upsert(dinner) }
        return MergeCount.of(decisions.map { it.second })
    }

    private fun decideDay(day: MealDayEntity, localUpdatedAt: Long?, knownDishTypes: Map<String, DishType>): MergeDecision =
        if (MergePolicy.isImportable(day, knownDishTypes)) MergePolicy.decide(localUpdatedAt, day.updatedAt)
        else MergeDecision.SKIP

    private fun decideDinner(dinner: DinnerDayEntity, localUpdatedAt: Long?, knownDishTypes: Map<String, DishType>): MergeDecision =
        if (MergePolicy.isImportable(dinner, knownDishTypes)) MergePolicy.decide(localUpdatedAt, dinner.updatedAt)
        else MergeDecision.SKIP
}
