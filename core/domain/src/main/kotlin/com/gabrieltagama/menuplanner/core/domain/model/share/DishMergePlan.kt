package com.gabrieltagama.menuplanner.core.domain.model.share

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId

/**
 * Decides how the dishes of an import land in the local recipe book. Each incoming dish is matched
 * by id and, when its id is unknown, by DishKey; a match by key keeps the local id, so the planned
 * days already stored keep pointing to it. ImportPolicy picks add, update or skip against the
 * matched dish. Newer incoming dishes are planned first, so when several of them share a key the
 * newest decides and the rest are skipped. idMap translates every incoming id to its local id, so
 * the planned days of the same snapshot are remapped before being resolved.
 */
class DishMergePlan private constructor(
    val writes: List<Dish>,
    val decisions: List<MergeDecision>,
    val idMap: Map<DishId, DishId>
) {

    companion object {
        fun of(local: List<Dish>, incoming: List<Dish>): DishMergePlan = Builder(local).apply { incoming.sortedByDescending(Dish::updatedAt).forEach(::plan) }.build()
    }

    private class Builder(local: List<Dish>) {
        private val known = local.associateByTo(mutableMapOf(), Dish::id)
        private val idsByKey = local.sortedBy(Dish::updatedAt).associateTo(mutableMapOf()) { DishKey.of(it) to it.id }
        private val writes = mutableListOf<Dish>()
        private val decisions = mutableListOf<MergeDecision>()
        private val idMap = mutableMapOf<DishId, DishId>()

        fun plan(dish: Dish) {
            val target = matchOf(dish)
            val decision = ImportPolicy.decide(target?.updatedAt, dish.updatedAt)
            val merged = dish.copy(id = target?.id ?: dish.id)
            idMap[dish.id] = merged.id
            decisions += decision
            if (decision != MergeDecision.SKIP) register(merged, replacing = target)
        }

        fun build(): DishMergePlan = DishMergePlan(writes.toList(), decisions.toList(), idMap.toMap())

        private fun matchOf(dish: Dish): Dish? = known[dish.id] ?: idsByKey[DishKey.of(dish)]?.let(known::get)

        private fun register(dish: Dish, replacing: Dish?) {
            replacing?.let { idsByKey.remove(DishKey.of(it), it.id) }
            known[dish.id] = dish
            idsByKey[DishKey.of(dish)] = dish.id
            writes += dish
        }
    }
}
