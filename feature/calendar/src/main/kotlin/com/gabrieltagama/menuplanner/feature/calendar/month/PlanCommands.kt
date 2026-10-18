package com.gabrieltagama.menuplanner.feature.calendar.month

import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.PastedDays
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillMonthDinnersUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillMonthUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearMealDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PasteDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PasteMealDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.RestoreDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.RestoreMealDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.UndoPasteDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.UndoPasteMealDaysUseCase
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Commands of the monthly calendar on the lunch or the dinner plan: each one runs the lunch or the
 * dinner use case of the view it acts on, so CalendarViewModel never branches on PlanView.
 * Deletions and pastes return what changed (PlanDeletion, PlanPaste) so they can be undone.
 */
class PlanCommands @Inject constructor(
    private val autoFillMonth: AutoFillMonthUseCase,
    private val autoFillMonthDinners: AutoFillMonthDinnersUseCase,
    private val clearMealDays: ClearMealDaysUseCase,
    private val clearDinnerDays: ClearDinnerDaysUseCase,
    private val restoreMealDays: RestoreMealDaysUseCase,
    private val restoreDinnerDays: RestoreDinnerDaysUseCase,
    private val pasteMealDays: PasteMealDaysUseCase,
    private val pasteDinnerDays: PasteDinnerDaysUseCase,
    private val undoPasteMealDays: UndoPasteMealDaysUseCase,
    private val undoPasteDinnerDays: UndoPasteDinnerDaysUseCase
) {
    suspend fun autoFill(month: YearMonth, view: PlanView): AutoFillResult = when (view) {
        PlanView.LUNCH -> autoFillMonth(month)
        PlanView.DINNER -> autoFillMonthDinners(month)
    }

    suspend fun autoFill(dates: Set<LocalDate>, view: PlanView): AutoFillResult = when (view) {
        PlanView.LUNCH -> autoFillMonth(dates)
        PlanView.DINNER -> autoFillMonthDinners(dates)
    }

    suspend fun delete(dates: Set<LocalDate>, view: PlanView): PlanDeletion = when (view) {
        PlanView.LUNCH -> PlanDeletion.Lunches(clearMealDays(dates))
        PlanView.DINNER -> PlanDeletion.Dinners(clearDinnerDays(dates))
    }

    suspend fun paste(clipboard: PlanClipboard, target: LocalDate): PlanPaste = when (clipboard.view) {
        PlanView.LUNCH -> PlanPaste.Lunches(pasteMealDays(clipboard.dates, target))
        PlanView.DINNER -> PlanPaste.Dinners(pasteDinnerDays(clipboard.dates, target))
    }

    suspend fun undo(deletion: PlanDeletion) {
        when (deletion) {
            is PlanDeletion.Lunches -> restoreMealDays(deletion.days)
            is PlanDeletion.Dinners -> restoreDinnerDays(deletion.days)
        }
    }

    suspend fun undo(paste: PlanPaste) {
        when (paste) {
            is PlanPaste.Lunches -> undoPasteMealDays(paste.days)
            is PlanPaste.Dinners -> undoPasteDinnerDays(paste.days)
        }
    }
}

sealed interface PlanPaste {
    val count: Int
    val view: PlanView

    data class Lunches(val days: PastedDays<MealDay>) : PlanPaste {
        override val count: Int get() = days.count
        override val view: PlanView get() = PlanView.LUNCH
    }

    data class Dinners(val days: PastedDays<DinnerDay>) : PlanPaste {
        override val count: Int get() = days.count
        override val view: PlanView get() = PlanView.DINNER
    }
}

sealed interface PlanDeletion {
    val count: Int
    val view: PlanView

    data class Lunches(val days: List<MealDay>) : PlanDeletion {
        override val count: Int get() = days.size
        override val view: PlanView get() = PlanView.LUNCH
    }

    data class Dinners(val days: List<DinnerDay>) : PlanDeletion {
        override val count: Int get() = days.size
        override val view: PlanView get() = PlanView.DINNER
    }
}
