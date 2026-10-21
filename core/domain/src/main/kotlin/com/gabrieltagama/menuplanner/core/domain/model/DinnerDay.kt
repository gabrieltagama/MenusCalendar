package com.gabrieltagama.menuplanner.core.domain.model

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import java.time.Instant
import java.time.LocalDate

/**
 * The dinner planned for one calendar day: always a single dinner option (Dish.isDinnerOption).
 * plan is the factory that checks that rule for a dinner chosen by hand or received in an import.
 */
data class DinnerDay(
    override val date: LocalDate,
    val dish: Dish,
    override val updatedAt: Instant
) : PlannedDay<DinnerDay> {
    override fun movedTo(date: LocalDate, updatedAt: Instant): DinnerDay = copy(date = date, updatedAt = updatedAt)

    companion object {
        fun plan(date: LocalDate, dish: Dish, updatedAt: Instant): Outcome<DinnerDay> =
            if (dish.isDinnerOption) Outcome.Success(DinnerDay(date = date, dish = dish, updatedAt = updatedAt)) else Outcome.Failure(DomainError.NotADinnerDish)
    }
}
