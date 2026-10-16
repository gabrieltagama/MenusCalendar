package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.ShoppingFilter
import com.gabrieltagama.menuplanner.core.domain.repository.ShoppingChecklistRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory ShoppingChecklistRepository keyed by filter.
 */
class FakeShoppingChecklistRepository(initial: Map<ShoppingFilter, Set<String>> = emptyMap()) : ShoppingChecklistRepository {
    val checkedByFilter = MutableStateFlow(initial)

    override fun checked(filter: ShoppingFilter): Flow<Set<String>> = checkedByFilter.map { it[filter].orEmpty() }

    override suspend fun setChecked(filter: ShoppingFilter, key: String, isChecked: Boolean) {
        val current = checkedByFilter.value[filter].orEmpty()
        checkedByFilter.value += filter to if (isChecked) current + key else current - key
    }

    override suspend fun clear(filter: ShoppingFilter) {
        checkedByFilter.value -= filter
    }

    override suspend fun clearEndedBefore(date: LocalDate) {
        checkedByFilter.value = checkedByFilter.value.filterKeys { !it.to.isBefore(date) }
    }
}
