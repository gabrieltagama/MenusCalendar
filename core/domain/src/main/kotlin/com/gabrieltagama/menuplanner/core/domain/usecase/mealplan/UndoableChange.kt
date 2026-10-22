package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.Meal

/**
 * What a deletion or a paste changed in the calendar of [meal]: how many days, and how to undo it.
 * PlanCommandService.undo runs the undo in a transaction.
 */
class UndoableChange(val meal: Meal, val count: Int, private val undoAction: suspend () -> Unit) {
    internal suspend fun undo() = undoAction()
}
