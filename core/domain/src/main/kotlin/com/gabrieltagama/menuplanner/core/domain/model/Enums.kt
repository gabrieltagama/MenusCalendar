package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Domain enumerations. Persisted and exported by name, so constants must never be renamed;
 * new values can be appended safely. MealTime says when a dish may be served: the calendar
 * plans lunches with LUNCH and ANY dishes and dinners with DINNER and ANY dishes. Ingredients
 * are kept in the STANDARD units (grams, millilitres or units); the other MeasureUnit values
 * remain only to read old data and shared files, and are converted on save, import and app start.
 * Meal names the two meals of a day that the calendar plans (lunch and dinner).
 */
enum class DishType { STARTER, MAIN, DESSERT, SINGLE }

enum class Heaviness { VERY_LOW, MEDIUM, VERY_HIGH }

enum class Meal { LUNCH, DINNER }

enum class MeasureUnit {
    GRAM, KILOGRAM, MILLILITER, LITER, UNIT, TABLESPOON, TEASPOON, CUP, PINCH, TO_TASTE;

    val isStandard: Boolean get() = this in STANDARD

    val isVolume: Boolean get() = this == MILLILITER || this == LITER

    companion object {
        val STANDARD: List<MeasureUnit> = listOf(GRAM, MILLILITER, UNIT)
    }
}

enum class Rating(val stars: Int) { ONE(1), TWO(2), THREE(3), FOUR(4), FIVE(5) }

enum class MealTime {
    LUNCH, DINNER, ANY;

    val isServedAtLunch: Boolean get() = this != DINNER

    val isServedAtDinner: Boolean get() = this != LUNCH
}
