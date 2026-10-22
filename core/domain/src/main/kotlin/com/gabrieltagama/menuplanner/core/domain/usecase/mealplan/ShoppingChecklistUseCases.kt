package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.ShoppingFilter
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import com.gabrieltagama.menuplanner.core.domain.port.ShoppingChecklistRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Commands of the shopping list checkboxes (read through ShoppingChecklistQueries): ticks belong to the ShoppingFilter (dates and meals)
 * they were made in, so changing the filter shows its own ticks and going back to a filter brings
 * its ticks back. They stay until the user unticks them, clears that filter or the filter's range
 * is over (ClearExpiredShoppingChecksUseCase).
 */
class SetShoppingItemCheckedUseCase @Inject constructor(private val repository: ShoppingChecklistRepository) {
    suspend operator fun invoke(filter: ShoppingFilter, item: ShoppingItem, isChecked: Boolean) = repository.setChecked(filter, item.key, isChecked)
}

class ClearShoppingChecksUseCase @Inject constructor(private val repository: ShoppingChecklistRepository) {
    suspend operator fun invoke(filter: ShoppingFilter) = repository.clear(filter)
}

class ClearExpiredShoppingChecksUseCase @Inject constructor(private val repository: ShoppingChecklistRepository, private val clock: Clock) {
    suspend operator fun invoke() = repository.clearEndedBefore(LocalDate.now(clock))
}
