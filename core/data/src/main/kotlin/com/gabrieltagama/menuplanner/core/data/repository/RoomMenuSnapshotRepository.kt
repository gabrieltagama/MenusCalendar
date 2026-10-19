package com.gabrieltagama.menuplanner.core.data.repository

import androidx.room.withTransaction
import com.gabrieltagama.menuplanner.core.data.database.MenuPlannerDatabase
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.data.mapper.toEntry
import com.gabrieltagama.menuplanner.core.data.mapper.toIngredientEntities
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.share.DinnerEntry
import com.gabrieltagama.menuplanner.core.domain.model.share.DishMergePlan
import com.gabrieltagama.menuplanner.core.domain.model.share.ImportPolicy
import com.gabrieltagama.menuplanner.core.domain.model.share.LunchEntry
import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import com.gabrieltagama.menuplanner.core.domain.model.share.MergeDecision
import com.gabrieltagama.menuplanner.core.domain.model.share.SnapshotScope
import com.gabrieltagama.menuplanner.core.domain.repository.MenuSnapshotRepository
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Room-backed snapshot of the recipe book and calendar. read is one consistent transaction; merge
 * runs in one transaction: dishes first with a DishMergePlan (match by id, else by name and type,
 * newest updatedAt wins), so the planned days can use dishes that arrived in the same snapshot;
 * then the lunches and dinners, remapped to the local dish ids, that resolve against the known
 * dishes with the domain rules (LunchEntry.resolve, DinnerEntry.resolve); the rest are skipped.
 */
internal class RoomMenuSnapshotRepository @Inject constructor(
    private val database: MenuPlannerDatabase
) : MenuSnapshotRepository {

    private val dishDao get() = database.dishDao()
    private val mealDayDao get() = database.mealDayDao()
    private val dinnerDayDao get() = database.dinnerDayDao()

    override suspend fun read(scope: SnapshotScope): MenuSnapshot = database.withTransaction {
        MenuSnapshot(
            dishes = allDishes(),
            lunches = if (scope == SnapshotScope.EVERYTHING) mealDayDao.getAll().map { it.toEntry() } else emptyList(),
            dinners = if (scope == SnapshotScope.EVERYTHING) dinnerDayDao.getAll().map { it.toEntry() } else emptyList()
        )
    }

    override suspend fun merge(snapshot: MenuSnapshot): ImportSummary = database.withTransaction {
        val plan = DishMergePlan.of(local = allDishes(), incoming = snapshot.dishes)
        dishDao.upsertAllWithIngredients(plan.writes.map { it.toEntity() to it.toIngredientEntities() })
        val known = allDishes().associateBy(Dish::id)
        val dayDecisions = mergeLunches(snapshot.lunches.map { it.remapped(plan.idMap) }, known) +
            mergeDinners(snapshot.dinners.map { it.remapped(plan.idMap) }, known)
        ImportPolicy.summaryOf(plan.decisions, dayDecisions)
    }

    private suspend fun allDishes(): List<Dish> = dishDao.getAll().map(DishWithIngredients::toDomain)

    private suspend fun mergeLunches(entries: List<LunchEntry>, known: Map<DishId, Dish>): List<MergeDecision> {
        val local = mealDayDao.getAll().associate { LocalDate.ofEpochDay(it.date) to Instant.ofEpochMilli(it.updatedAt) }
        val decisions = entries.map { entry -> entry.resolve(known) to entry }
            .map { (day, entry) -> day to if (day == null) MergeDecision.SKIP else ImportPolicy.decide(local[entry.date], entry.updatedAt) }
        mealDayDao.upsertAll(decisions.filter { (_, decision) -> decision != MergeDecision.SKIP }.mapNotNull { (day, _) -> day?.toEntity() })
        return decisions.map { it.second }
    }

    private suspend fun mergeDinners(entries: List<DinnerEntry>, known: Map<DishId, Dish>): List<MergeDecision> {
        val local = dinnerDayDao.getAll().associate { LocalDate.ofEpochDay(it.date) to Instant.ofEpochMilli(it.updatedAt) }
        val decisions = entries.map { entry -> entry.resolve(known) to entry }
            .map { (day, entry) -> day to if (day == null) MergeDecision.SKIP else ImportPolicy.decide(local[entry.date], entry.updatedAt) }
        dinnerDayDao.upsertAll(decisions.filter { (_, decision) -> decision != MergeDecision.SKIP }.mapNotNull { (day, _) -> day?.toEntity() })
        return decisions.map { it.second }
    }
}
