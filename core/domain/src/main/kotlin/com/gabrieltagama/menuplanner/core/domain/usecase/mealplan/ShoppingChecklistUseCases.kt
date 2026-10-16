package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import com.gabrieltagama.menuplanner.core.domain.repository.ShoppingChecklistRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use cases of the shopping list checkboxes: products stay ticked, whatever the date range or
 * meals shown, until the user unticks them or clears the whole checklist.
 */
class ObserveShoppingChecksUseCase @Inject constructor(private val repository: ShoppingChecklistRepository) {
    operator fun invoke(): Flow<Set<String>> = repository.checked
}

class SetShoppingItemCheckedUseCase @Inject constructor(private val repository: ShoppingChecklistRepository) {
    suspend operator fun invoke(item: ShoppingItem, isChecked: Boolean) = repository.setChecked(item.key, isChecked)
}

class ClearShoppingChecksUseCase @Inject constructor(private val repository: ShoppingChecklistRepository) {
    suspend operator fun invoke() = repository.clear()
}
