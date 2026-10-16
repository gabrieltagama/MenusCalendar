package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.ShoppingFilter
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import com.gabrieltagama.menuplanner.core.domain.repository.ShoppingChecklistRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use cases of the shopping list checkboxes: ticks belong to the ShoppingFilter (dates and meals)
 * they were made in, so changing the filter shows its own ticks and going back to a filter brings
 * its ticks back. They stay until the user unticks them, clears that filter or the filter's range
 * is over (ClearExpiredShoppingChecksUseCase).
 */
class ObserveShoppingChecksUseCase @Inject constructor(private val repository: ShoppingChecklistRepository) {
    operator fun invoke(filter: ShoppingFilter): Flow<Set<String>> = repository.checked(filter)
}

class SetShoppingItemCheckedUseCase @Inject constructor(private val repository: ShoppingChecklistRepository) {
    suspend operator fun invoke(filter: ShoppingFilter, item: ShoppingItem, isChecked: Boolean) = repository.setChecked(filter, item.key, isChecked)
}

class ClearShoppingChecksUseCase @Inject constructor(private val repository: ShoppingChecklistRepository) {
    suspend operator fun invoke(filter: ShoppingFilter) = repository.clear(filter)
}

class ClearExpiredShoppingChecksUseCase @Inject constructor(private val repository: ShoppingChecklistRepository, private val clock: Clock) {
    suspend operator fun invoke() = repository.clearEndedBefore(LocalDate.now(clock))
}
