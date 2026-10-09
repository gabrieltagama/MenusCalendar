package com.gabrieltagama.menuplanner.feature.calendar.month

import app.cash.turbine.test
import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillMonthUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ObserveMonthPlanUseCase
import com.gabrieltagama.menuplanner.feature.calendar.testing.FakeDishRepository
import com.gabrieltagama.menuplanner.feature.calendar.testing.FakeMealPlanRepository
import com.gabrieltagama.menuplanner.feature.calendar.testing.MainDispatcherRule
import com.gabrieltagama.menuplanner.feature.calendar.testing.awaitItemMatching
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * CalendarViewModel over the real ObserveMonthPlanUseCase with a fixed clock (2026-10-09 UTC):
 * month navigation, selected day summary, the "today" shortcut and the month autofill over the
 * real AutoFillMonthUseCase with in-memory repositories and a seeded Random.
 */
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 9)
    private val clock = Clock.fixed(Instant.parse("2026-10-09T10:00:00Z"), ZoneOffset.UTC)

    private val starter = Dish(id = "gazpacho", name = "Gazpacho", type = DishType.STARTER, heaviness = Heaviness.VERY_LOW)
    private val main = Dish(id = "chicken", name = "Pollo asado", type = DishType.MAIN, heaviness = Heaviness.MEDIUM)
    private val todayPlan = MealDay(date = today, menu = DailyMenu.Courses(starter = starter, main = main))
    private val novemberPlan = MealDay(date = LocalDate.of(2026, 11, 3), menu = DailyMenu.Single(single = main.copy(type = DishType.SINGLE)))

    private val repository = FakeMealPlanRepository(listOf(todayPlan, novemberPlan))
    private var dishRepository = FakeDishRepository(listOf(starter, main))
    private val viewModel by lazy {
        CalendarViewModel(
            ObserveMonthPlanUseCase(repository),
            AutoFillMonthUseCase(dishRepository, repository, Random(42), clock),
            clock
        )
    }

    @Test
    fun `starts on current month with today selected`() = runTest {
        viewModel.uiState.test {
            val state = awaitItemMatching { !it.isLoading }

            assertEquals(YearMonth.of(2026, 10), state.month)
            assertEquals(today, state.today)
            assertEquals(today, state.selectedDate)
            assertEquals(mapOf(today to todayPlan), state.days)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selected day summary exposes the planned day`() = runTest {
        viewModel.uiState.test {
            val state = awaitItemMatching { !it.isLoading }

            assertEquals(todayPlan, state.selectedDay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting a day without plan clears summary`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }
            val emptyDate = LocalDate.of(2026, 10, 20)

            viewModel.onDateSelect(emptyDate)

            val state = awaitItemMatching { it.selectedDate == emptyDate }
            assertNull(state.selectedDay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `next month loads its plan and selects first day`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onNextMonth()

            val state = awaitItemMatching {
                it.month == YearMonth.of(2026, 11) && !it.isLoading && it.selectedDate == LocalDate.of(2026, 11, 1)
            }
            assertEquals(setOf(novemberPlan.date), state.days.keys)
            assertNull(state.selectedDay)
            assertEquals(today, state.today)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `previous month selects first day and has no plan`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onPreviousMonth()

            val state = awaitItemMatching {
                it.month == YearMonth.of(2026, 9) && !it.isLoading && it.selectedDate == LocalDate.of(2026, 9, 1)
            }
            assertTrue(state.days.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `navigating back to the current month selects today`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onNextMonth()
            viewModel.onPreviousMonth()

            val state = awaitItemMatching { it.month == YearMonth.of(2026, 10) && !it.isLoading && it.selectedDate == today }
            assertEquals(todayPlan, state.selectedDay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `today returns to current month and date`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }
            viewModel.onNextMonth()
            viewModel.onNextMonth()
            awaitItemMatching { it.month == YearMonth.of(2026, 12) }

            viewModel.onToday()

            val state = awaitItemMatching { it.month == YearMonth.of(2026, 10) && !it.isLoading && it.selectedDate == today }
            assertEquals(todayPlan, state.selectedDay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `plan changes are reflected while observing`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            repository.clear(today)

            val state = awaitItemMatching { it.days.isEmpty() && !it.isLoading }
            assertNull(state.selectedDay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `autofill request shows confirmation and dismiss hides it`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onAutoFillRequest()
            awaitItemMatching { it.showAutoFillConfirm }
            viewModel.onAutoFillDismiss()

            val state = awaitItemMatching { !it.showAutoFillConfirm }
            assertFalse(state.isAutoFilling)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `autofill fills empty days of visible month and emits result`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onAutoFillRequest()
            awaitItemMatching { it.showAutoFillConfirm }
            viewModel.events.test {
                viewModel.onAutoFill()

                val event = awaitItem() as CalendarEvent.AutoFillCompleted
                assertEquals(30, event.result.filledDays)
                assertEquals(0, event.result.unfilledDays)
            }

            val state = awaitItemMatching { it.days.size == 31 && !it.isAutoFilling }
            assertFalse(state.showAutoFillConfirm)
            assertEquals(todayPlan, state.selectedDay)
            assertTrue(repository.saved.all { YearMonth.from(it.date) == YearMonth.of(2026, 10) })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `autofill targets the month being displayed`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }
            viewModel.onNextMonth()
            awaitItemMatching { it.month == YearMonth.of(2026, 11) && !it.isLoading }

            viewModel.events.test {
                viewModel.onAutoFill()

                val event = awaitItem() as CalendarEvent.AutoFillCompleted
                assertEquals(29, event.result.filledDays)
            }

            assertTrue(repository.saved.all { YearMonth.from(it.date) == YearMonth.of(2026, 11) })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `autofill without dishes reports every empty day as unfilled`() = runTest {
        dishRepository = FakeDishRepository()

        viewModel.events.test {
            viewModel.onAutoFill()

            assertEquals(CalendarEvent.AutoFillCompleted(AutoFillResult(0, 0, 0, 30)), awaitItem())
        }
        assertTrue(repository.saved.isEmpty())
    }
}
