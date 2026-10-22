package com.gabrieltagama.menuplanner.core.domain.usecase.share

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.PlannedDay
import com.gabrieltagama.menuplanner.core.domain.model.share.DishMergePlan
import com.gabrieltagama.menuplanner.core.domain.model.share.ImportPolicy
import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import com.gabrieltagama.menuplanner.core.domain.model.share.MergeDecision
import com.gabrieltagama.menuplanner.core.domain.model.share.PlanEntry
import com.gabrieltagama.menuplanner.core.domain.port.DayPlanRepository
import com.gabrieltagama.menuplanner.core.domain.port.DinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import com.gabrieltagama.menuplanner.core.domain.port.LunchPlanRepository
import javax.inject.Inject

/**
 * Domain service that merges a received snapshot into the local recipe book and calendar, the same
 * way for a shared file, a restored backup and the starter recipe book. Dishes go first with a
 * DishMergePlan (matched by id or, failing that, by DishKey; the newest updatedAt wins), so planned
 * days can use dishes that arrived in the same snapshot. Then lunches and dinners, remapped to the
 * local dish ids, are resolved against the known dishes with the planning rules (PlanEntry.resolve)
 * and merged with ImportPolicy; the ones that break the rules are skipped. The caller runs it inside
 * a transaction.
 */
class MenuSnapshotMerger @Inject constructor(
    private val dishes: DishRepository,
    private val lunches: LunchPlanRepository,
    private val dinners: DinnerPlanRepository
) {
    suspend fun merge(snapshot: MenuSnapshot): ImportSummary {
        val plan = DishMergePlan.of(local = dishes.getDishes(), incoming = snapshot.dishes)
        if (plan.writes.isNotEmpty()) dishes.upsertAll(plan.writes)
        val known = dishes.getDishes().associateBy(Dish::id)
        val dayDecisions = mergeDays(snapshot.lunches.map { it.remapped(plan.idMap) }, known, lunches) +
            mergeDays(snapshot.dinners.map { it.remapped(plan.idMap) }, known, dinners)
        return ImportPolicy.summaryOf(plan.decisions, dayDecisions)
    }

    private suspend fun <D : PlannedDay<D>> mergeDays(
        entries: List<PlanEntry<D>>,
        known: Map<DishId, Dish>,
        repository: DayPlanRepository<D>
    ): List<MergeDecision> {
        if (entries.isEmpty()) return emptyList()
        val local = repository.getRange(entries.minOf { it.date }..entries.maxOf { it.date }).associate { it.date to it.updatedAt }
        val decisions = entries.map { entry ->
            val day = entry.resolve(known)
            day to if (day == null) MergeDecision.SKIP else ImportPolicy.decide(local[entry.date], entry.updatedAt)
        }
        val accepted = decisions.filter { (_, decision) -> decision != MergeDecision.SKIP }.mapNotNull { (day, _) -> day }
        if (accepted.isNotEmpty()) repository.saveAll(accepted)
        return decisions.map { (_, decision) -> decision }
    }
}
