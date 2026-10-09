package com.gabrieltagama.menuplanner.feature.calendar.month

import android.content.res.Resources
import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.feature.calendar.R

/**
 * Builds the Spanish summary shown after the month autofill, e.g. "Rellenados 18 días, 2 repiten
 * plato, 3 sin rellenar (faltan platos)". Texts come from [AutoFillTexts] so the composition can
 * be unit tested without Android resources; [ResourcesAutoFillTexts] reads the plurals.
 */
interface AutoFillTexts {
    val nothingToFill: String
    val notEnoughDishes: String
    fun filled(count: Int): String
    fun repeatedDish(count: Int): String
    fun heavyStreak(count: Int): String
    fun unfilled(count: Int): String
}

class AutoFillMessageFormatter(private val texts: AutoFillTexts) {

    fun format(result: AutoFillResult): String = when {
        result.filledDays == 0 && result.unfilledDays == 0 -> texts.nothingToFill
        result.filledDays == 0 -> texts.notEnoughDishes
        else -> summaryParts(result).joinToString(SEPARATOR)
    }

    private fun summaryParts(result: AutoFillResult): List<String> = listOfNotNull(
        texts.filled(result.filledDays),
        result.repeatedDishDays.takeIf { it > 0 }?.let(texts::repeatedDish),
        result.heavyStreakDays.takeIf { it > 0 }?.let(texts::heavyStreak),
        result.unfilledDays.takeIf { it > 0 }?.let(texts::unfilled)
    )

    private companion object {
        const val SEPARATOR = ", "
    }
}

class ResourcesAutoFillTexts(private val resources: Resources) : AutoFillTexts {
    override val nothingToFill: String get() = resources.getString(R.string.calendar_autofill_nothing_to_fill)
    override val notEnoughDishes: String get() = resources.getString(R.string.calendar_autofill_not_enough_dishes)
    override fun filled(count: Int): String = plural(R.plurals.calendar_autofill_filled, count)
    override fun repeatedDish(count: Int): String = plural(R.plurals.calendar_autofill_repeated_dish, count)
    override fun heavyStreak(count: Int): String = plural(R.plurals.calendar_autofill_heavy_streak, count)
    override fun unfilled(count: Int): String = plural(R.plurals.calendar_autofill_unfilled, count)

    private fun plural(id: Int, count: Int): String = resources.getQuantityString(id, count, count)
}
