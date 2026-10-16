package com.gabrieltagama.menuplanner.core.domain.model

/**
 * When a dish may be served: the calendar plans lunches with LUNCH and ANY dishes and dinners with
 * DINNER and ANY dishes. Persisted by name.
 */
enum class MealTime {
    LUNCH, DINNER, ANY;

    val isServedAtLunch: Boolean get() = this != DINNER

    val isServedAtDinner: Boolean get() = this != LUNCH
}
