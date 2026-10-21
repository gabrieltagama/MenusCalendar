package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.port.DishUsageRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory DishUsageRepository: every recorded serving date, filtered by the requested limit.
 */
class FakeDishUsageRepository(initial: Map<DishId, List<LocalDate>> = emptyMap()) : DishUsageRepository {
    val servings = MutableStateFlow(initial)

    override fun observeLastServed(until: LocalDate): Flow<Map<DishId, LocalDate>> =
        servings.map { all -> all.mapNotNull { (id, dates) -> dates.filter { it <= until }.maxOrNull()?.let { id to it } }.toMap() }
}
