package com.gabrieltagama.menuplanner.core.domain.model

import java.time.LocalDate

/**
 * A calendar day where a dish is planned, in a lunch slot or as the dinner. allows tells whether
 * the dish could stay there with another type and meal time, with the same rules as planning it by
 * hand: a lunch slot takes only its dish type and a dinner only a dinner option.
 */
sealed interface PlannedUse {
    val date: LocalDate

    fun allows(type: DishType, mealTime: MealTime): Boolean

    data class Lunch(override val date: LocalDate, val slot: MenuSlot) : PlannedUse {
        override fun allows(type: DishType, mealTime: MealTime): Boolean = slot.dishType == type
    }

    data class Dinner(override val date: LocalDate) : PlannedUse {
        override fun allows(type: DishType, mealTime: MealTime): Boolean = Dish.isDinnerOption(type, mealTime)
    }
}
