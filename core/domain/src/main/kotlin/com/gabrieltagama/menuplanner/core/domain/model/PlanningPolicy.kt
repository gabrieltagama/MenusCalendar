package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Heaviness rule shared by the lunch autofill: a lunch with a VERY_HIGH dish is a heavy day that
 * should not be next to another one. How many days count as recent is the user setting RepeatWindow.
 */
object PlanningPolicy {
    fun isHeavy(dish: Dish): Boolean = dish.heaviness == Heaviness.VERY_HIGH

    fun isHeavyDay(menu: DailyMenu?): Boolean = menu?.dishes.orEmpty().any(::isHeavy)
}
