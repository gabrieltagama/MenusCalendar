package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import app.cash.turbine.test
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishUsageRepository
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests that ObserveDishUsageUseCase counts only days up to today and moves "today" forward after
 * midnight without a new subscription, so a dish planned for tomorrow counts once that day arrives.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveDishUsageUseCaseTest {
    private val october9 = LocalDate.of(2026, 10, 9)
    private val october10 = LocalDate.of(2026, 10, 10)
    private val dishId = DishId("lentils")

    private var now = Instant.parse("2026-10-09T23:59:00Z")
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = now
    }

    @Test
    fun `refreshes today after midnight`() = runTest {
        val repository = FakeDishUsageRepository(mapOf(dishId to listOf(october9.minusDays(3), october10)))

        ObserveDishUsageUseCase(repository, clock)().test {
            val before = awaitItem()
            assertEquals(october9, before.today)
            assertEquals(october9.minusDays(3), before.lastServedOf(dishId))

            now = Instant.parse("2026-10-10T00:00:01Z")
            advanceTimeBy(MINUTE_AND_MARGIN_MILLIS)

            val after = awaitItem()
            assertEquals(october10, after.today)
            assertEquals(october10, after.lastServedOf(dishId))
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        const val MINUTE_AND_MARGIN_MILLIS = 61_001L
    }
}
