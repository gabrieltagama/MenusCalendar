package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.model.DishUsage
import com.gabrieltagama.menuplanner.core.domain.port.DishUsageRepository
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Observes when each dish was last served, counting today and every past day. "Today" is read again
 * right after every midnight of the clock's zone, so a screen left open overnight counts the new day.
 */
class ObserveDishUsageUseCase @Inject constructor(
    private val repository: DishUsageRepository,
    private val clock: Clock
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<DishUsage> =
        currentDate().flatMapLatest { today -> repository.observeLastServed(today).map { DishUsage(today = today, lastServed = it) } }

    private fun currentDate(): Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now(clock))
            delay(untilNextDay().toMillis())
        }
    }.distinctUntilChanged()

    private fun untilNextDay(): Duration {
        val now = ZonedDateTime.now(clock)
        return Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(now.zone)).plus(MIDNIGHT_MARGIN)
    }

    private companion object {
        val MIDNIGHT_MARGIN: Duration = Duration.ofSeconds(1)
    }
}
