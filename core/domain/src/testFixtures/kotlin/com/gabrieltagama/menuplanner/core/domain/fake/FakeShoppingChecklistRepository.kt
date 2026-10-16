package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.repository.ShoppingChecklistRepository
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory ShoppingChecklistRepository.
 */
class FakeShoppingChecklistRepository(initial: Set<String> = emptySet()) : ShoppingChecklistRepository {
    override val checked = MutableStateFlow(initial)

    override suspend fun setChecked(key: String, isChecked: Boolean) {
        checked.value = if (isChecked) checked.value + key else checked.value - key
    }

    override suspend fun clear() {
        checked.value = emptySet()
    }
}
