package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Outcome of the random autofill of a month or of some dates. filledDays were planned;
 * repeatedDishDays reuse a dish served within the user's RepeatWindow and heavyStreakDays follow
 * another VERY_HIGH day, both because no dish met the rule; unfilledDays stayed empty because no
 * menu could be built. EMPTY is the result when there was nothing to fill.
 */
data class AutoFillResult(
    val filledDays: Int,
    val repeatedDishDays: Int,
    val heavyStreakDays: Int,
    val unfilledDays: Int
) {
    companion object {
        val EMPTY = AutoFillResult(0, 0, 0, 0)
    }
}
