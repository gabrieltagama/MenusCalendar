package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import java.time.LocalDate
import kotlin.random.Random

/**
 * Domain service that builds a random menu for one day. Rules, from strict to relaxed:
 * a dish served within RECENT_DAYS days before or after the date is avoided, and next to a
 * heavy day (a main dish VERY_HIGH; desserts do not count), before or after, no VERY_HIGH main
 * dish is chosen. When no dish meets both rules, repeating is allowed first and only then a
 * heavy streak. Shapes (starter + main or single) are chosen from the first non-empty tier:
 * buildable without breaking rules, buildable by only repeating, any available. Dessert is
 * added at random.
 */
internal class MenuPicker(dishes: List<Dish>, private val random: Random) {

    private val dishesByType: Map<DishType, List<Dish>> = dishes.groupBy(Dish::type)

    fun pick(date: LocalDate, history: Map<LocalDate, DailyMenu>): MenuPick? {
        val context = PickContext(
            nearbyDishIds = nearbyDishIds(date, history),
            neighbourDayHeavy = history[date.minusDays(1)].isHeavy() || history[date.plusDays(1)].isHeavy()
        )
        val shapes = MenuShape.entries.filter { shape -> shape.isBuildable { candidatesOf(it) } }
        if (shapes.isEmpty()) return null
        val strictShapes = shapes.filter { shape -> shape.isBuildable { type -> candidatesOf(type).filter { context.isFresh(it) && context.isLightEnough(it) } } }
        val lightShapes = shapes.filter { shape -> shape.isBuildable { type -> candidatesOf(type).filter(context::isLightEnough) } }
        return build(strictShapes.ifEmpty { lightShapes }.ifEmpty { shapes }.random(random), context)
    }

    private fun build(shape: MenuShape, context: PickContext): MenuPick {
        val mains = shape.requiredTypes.map { pickSlot(it, context, checkHeaviness = true) }
        val dessert = pickDessert(context)
        val menu = when (shape) {
            MenuShape.COURSES -> DailyMenu.Courses(starter = mains[0].dish, main = mains[1].dish, dessert = dessert?.dish)
            MenuShape.SINGLE -> DailyMenu.Single(single = mains[0].dish, dessert = dessert?.dish)
        }
        val slots = mains + listOfNotNull(dessert)
        return MenuPick(menu, repeated = slots.any(SlotPick::repeated), breaksHeavyRule = slots.any(SlotPick::breaksHeavyRule))
    }

    private fun pickDessert(context: PickContext): SlotPick? =
        if (random.nextBoolean() && candidatesOf(DishType.DESSERT).isNotEmpty()) pickSlot(DishType.DESSERT, context, checkHeaviness = false) else null

    private fun pickSlot(type: DishType, context: PickContext, checkHeaviness: Boolean): SlotPick {
        val candidates = candidatesOf(type)
        val pool = candidates.filter { context.isFresh(it) && (!checkHeaviness || context.isLightEnough(it)) }
            .ifEmpty { candidates.filter { !checkHeaviness || context.isLightEnough(it) } }
            .ifEmpty { candidates }
        val dish = pool.random(random)
        return SlotPick(dish, repeated = !context.isFresh(dish), breaksHeavyRule = checkHeaviness && !context.isLightEnough(dish))
    }

    private fun MenuShape.isBuildable(candidates: (DishType) -> List<Dish>): Boolean =
        requiredTypes.all { candidates(it).isNotEmpty() }

    private fun candidatesOf(type: DishType): List<Dish> = dishesByType[type].orEmpty()

    private fun nearbyDishIds(date: LocalDate, history: Map<LocalDate, DailyMenu>): Set<String> =
        (1..RECENT_DAYS).flatMap { listOf(date.minusDays(it), date.plusDays(it)) }
            .mapNotNull(history::get)
            .flatMap(DailyMenu::dishes)
            .map(Dish::id)
            .toSet()

    private fun DailyMenu?.isHeavy(): Boolean = this?.mainDishes.orEmpty().any { it.heaviness == Heaviness.VERY_HIGH }

    companion object {
        const val RECENT_DAYS = 6L
    }
}

internal enum class MenuShape(val requiredTypes: List<DishType>) {
    COURSES(listOf(DishType.STARTER, DishType.MAIN)),
    SINGLE(listOf(DishType.SINGLE))
}

internal data class MenuPick(val menu: DailyMenu, val repeated: Boolean, val breaksHeavyRule: Boolean)

private data class SlotPick(val dish: Dish, val repeated: Boolean, val breaksHeavyRule: Boolean)

private data class PickContext(val nearbyDishIds: Set<String>, val neighbourDayHeavy: Boolean) {
    fun isFresh(dish: Dish): Boolean = dish.id !in nearbyDishIds
    fun isLightEnough(dish: Dish): Boolean = !neighbourDayHeavy || dish.heaviness != Heaviness.VERY_HIGH
}
