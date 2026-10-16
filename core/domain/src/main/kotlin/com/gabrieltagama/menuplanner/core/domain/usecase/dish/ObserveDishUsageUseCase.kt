package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.model.DishUsage
import com.gabrieltagama.menuplanner.core.domain.repository.DishUsageRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Observes when each dish was last served, counting today and every past day.
 */
class ObserveDishUsageUseCase @Inject constructor(
    private val repository: DishUsageRepository,
    private val clock: Clock
) {
    operator fun invoke(): Flow<DishUsage> {
        val today = LocalDate.now(clock)
        return repository.observeLastServed(today).map { DishUsage(today = today, lastServed = it) }
    }
}
