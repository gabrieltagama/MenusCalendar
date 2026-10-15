package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Domain enumerations. Persisted and exported by name, so constants must never be renamed;
 * new values can be appended safely. MealTime says when a dish may be served: the calendar
 * plans lunches, so only LUNCH and ANY dishes are eligible for it. A rating reaches a minimum
 * when it has at least as many stars.
 */
enum class DishType { STARTER, MAIN, DESSERT, SINGLE }

enum class Heaviness { VERY_LOW, MEDIUM, VERY_HIGH }

enum class MeasureUnit { GRAM, KILOGRAM, MILLILITER, LITER, UNIT, TABLESPOON, TEASPOON, CUP, PINCH, TO_TASTE }

enum class Rating(val stars: Int) {
    ONE(1), TWO(2), THREE(3), FOUR(4), FIVE(5);

    fun reaches(minimum: Rating): Boolean = stars >= minimum.stars
}

enum class MealTime {
    LUNCH, DINNER, ANY;

    val isServedAtLunch: Boolean get() = isServedAt(LUNCH)

    fun isServedAt(time: MealTime): Boolean = this == ANY || this == time
}
