package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Outcome of the random autofill of a month. filledDays were planned; repeatedDishDays reuse a
 * dish served in the previous 6 days and heavyStreakDays follow another VERY_HIGH day, both
 * because no dish met the rule; unfilledDays stayed empty because no menu shape could be built.
 */
data class AutoFillResult(
    val filledDays: Int,
    val repeatedDishDays: Int,
    val heavyStreakDays: Int,
    val unfilledDays: Int
)
