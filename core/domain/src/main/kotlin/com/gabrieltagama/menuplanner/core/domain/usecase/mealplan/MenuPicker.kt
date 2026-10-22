package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.MenuShape
import com.gabrieltagama.menuplanner.core.domain.model.PlanningPolicy
import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import java.time.LocalDate
import kotlin.random.Random

/**
 * Domain service that builds a random menu for one day. The dinner of that same day is never used
 * unless a course has no other dish. Then, from strict to relaxed (PlanningPolicy, RepeatWindow): a
 * recent dish (served at lunch or dinner within the window before or after the date) is avoided,
 * and next to a heavy day, before or after, no VERY_HIGH dish is chosen. When no dish meets both
 * rules, repeating is allowed first and only then a heavy streak. Shapes (starter + main or
 * single) are chosen from the first non-empty tier: buildable without breaking rules, buildable
 * by only repeating, any available. Desserts are never planned.
 */
internal class MenuPicker(dishes: List<Dish>, private val window: RepeatWindow, private val random: Random) {

    private val dishesByType: Map<DishType, List<Dish>> = dishes.groupBy(Dish::type)

    fun pick(date: LocalDate, history: PlanHistory): MenuPick? {
        val context = PickContext(
            dinnerOfDay = history.dinnerOf(date)?.id,
            nearbyDishIds = history.dishIdsNear(date, window),
            neighbourDayHeavy = history.isNextToHeavyDay(date)
        )
        val shapes = MenuShape.entries.filter { shape -> shape.isBuildable { candidatesOf(it, context) } }
        if (shapes.isEmpty()) return null
        val strictShapes = shapes.filter { shape -> shape.isBuildable { type -> candidatesOf(type, context).filter { context.isFresh(it) && context.isLightEnough(it) } } }
        val lightShapes = shapes.filter { shape -> shape.isBuildable { type -> candidatesOf(type, context).filter(context::isLightEnough) } }
        return build(strictShapes.ifEmpty { lightShapes }.ifEmpty { shapes }.random(random), context)
    }

    private fun build(shape: MenuShape, context: PickContext): MenuPick {
        val picks = shape.slots.associateWith { pickSlot(it.dishType, context) }
        val menu = checkNotNull(DailyMenu.of(shape, picks.mapValues { (_, pick) -> pick.dish }))
        return MenuPick(menu, repeated = picks.values.any(SlotPick::repeated), breaksHeavyRule = picks.values.any(SlotPick::breaksHeavyRule))
    }

    private fun pickSlot(type: DishType, context: PickContext): SlotPick {
        val candidates = candidatesOf(type, context)
        val pool = candidates.filter { context.isFresh(it) && context.isLightEnough(it) }
            .ifEmpty { candidates.filter(context::isLightEnough) }
            .ifEmpty { candidates }
        val dish = pool.random(random)
        return SlotPick(dish, repeated = !context.isFresh(dish) || dish.id == context.dinnerOfDay, breaksHeavyRule = !context.isLightEnough(dish))
    }

    private fun MenuShape.isBuildable(candidates: (DishType) -> List<Dish>): Boolean =
        requiredTypes.all { candidates(it).isNotEmpty() }

    private fun candidatesOf(type: DishType, context: PickContext): List<Dish> {
        val all = dishesByType[type].orEmpty()
        return all.filterNot { it.id == context.dinnerOfDay }.ifEmpty { all }
    }
}

internal data class MenuPick(val menu: DailyMenu, val repeated: Boolean, val breaksHeavyRule: Boolean)

private data class SlotPick(val dish: Dish, val repeated: Boolean, val breaksHeavyRule: Boolean)

private data class PickContext(val dinnerOfDay: DishId?, val nearbyDishIds: Set<DishId>, val neighbourDayHeavy: Boolean) {
    fun isFresh(dish: Dish): Boolean = dish.id !in nearbyDishIds
    fun isLightEnough(dish: Dish): Boolean = !neighbourDayHeavy || !PlanningPolicy.isHeavy(dish)
}
