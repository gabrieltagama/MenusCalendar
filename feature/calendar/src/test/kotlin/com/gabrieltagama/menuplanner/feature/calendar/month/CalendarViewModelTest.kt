package com.gabrieltagama.menuplanner.feature.calendar.month

import app.cash.turbine.test
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakePlanQueries
import com.gabrieltagama.menuplanner.core.domain.fake.FakeSettingsRepository
import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.plan.toPlannedDinner
import com.gabrieltagama.menuplanner.core.domain.model.plan.toPlannedLunch
import com.gabrieltagama.menuplanner.core.domain.testing.MainDispatcherRule
import com.gabrieltagama.menuplanner.core.domain.testing.awaitItemMatching
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillMonthDinnersUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillMonthUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearMealDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ObserveMonthDinnersUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ObserveMonthPlanUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PasteDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PasteMealDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.RestoreDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.RestoreMealDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.UndoPasteDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.UndoPasteMealDaysUseCase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
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
 * CalendarViewModel over the real ObserveMonthPlanUseCase (read through FakePlanQueries) with a controllable clock (2026-10-09 UTC):
 * month navigation, selected day summary, the "today" shortcut and the month autofill over the
 * real AutoFillMonthUseCase with in-memory repositories and a seeded Random; the lunches/dinners
 * view switch, the dinner autofill and the refresh of "today" when the screen resumes; the selection
 * mode (tap, drag, select all, view change) and its actions on the active view: bulk deletion with
 * undo, autofill of the marked empty days and copy/paste with undo.
 */
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 9)
    private var now = Instant.parse("2026-10-09T10:00:00Z")
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = now
    }

    private val starter = Dish(id = DishId("gazpacho"), name = "Gazpacho", type = DishType.STARTER, heaviness = Heaviness.VERY_LOW, updatedAt = Instant.EPOCH)
    private val main = Dish(id = DishId("chicken"), name = "Pollo asado", type = DishType.MAIN, heaviness = Heaviness.MEDIUM, updatedAt = Instant.EPOCH)
    private val todayPlan = MealDay(date = today, menu = DailyMenu.Courses(starter = starter, main = main), updatedAt = Instant.EPOCH)
    private val novemberPlan = MealDay(date = LocalDate.of(2026, 11, 3), menu = DailyMenu.Single(single = main.copy(type = DishType.SINGLE)), updatedAt = Instant.EPOCH)

    private val todayDinner = DinnerDay(date = today, dish = main, updatedAt = Instant.EPOCH)
    private val nextPlan = todayPlan.copy(date = today.plusDays(2))

    private val repository = FakeMealPlanRepository(listOf(todayPlan, novemberPlan))
    private val dinnerRepository = FakeDinnerPlanRepository(listOf(todayDinner))
    private var dishRepository = FakeDishRepository(listOf(starter, main))
    private val settingsRepository = FakeSettingsRepository()
    private val queries = FakePlanQueries(repository, dinnerRepository)
    private val viewModel by lazy {
        CalendarViewModel(
            ObserveMonthPlanUseCase(queries),
            ObserveMonthDinnersUseCase(queries),
            AutoFillMonthUseCase(dishRepository, repository, dinnerRepository, settingsRepository, Random(42), clock),
            AutoFillMonthDinnersUseCase(dishRepository, repository, dinnerRepository, settingsRepository, Random(42), clock),
            ClearMealDaysUseCase(repository),
            ClearDinnerDaysUseCase(dinnerRepository),
            RestoreMealDaysUseCase(repository),
            RestoreDinnerDaysUseCase(dinnerRepository),
            PasteMealDaysUseCase(repository, clock),
            PasteDinnerDaysUseCase(dinnerRepository, clock),
            UndoPasteMealDaysUseCase(repository),
            UndoPasteDinnerDaysUseCase(dinnerRepository),
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
            assertEquals(mapOf(today to todayPlan.toPlannedLunch()), state.days)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resuming the screen after midnight refreshes today`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }
            now = Instant.parse("2026-10-10T00:05:00Z")
            viewModel.onScreenResume()

            assertEquals(today.plusDays(1), awaitItemMatching { it.today != today }.today)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selected day summary exposes the planned day`() = runTest {
        viewModel.uiState.test {
            val state = awaitItemMatching { !it.isLoading }

            assertEquals(todayPlan.toPlannedLunch(), state.selectedDay)
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
            assertEquals(todayPlan.toPlannedLunch(), state.selectedDay)
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
            assertEquals(todayPlan.toPlannedLunch(), state.selectedDay)
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
    fun `autofill fills empty days of the current month from today and emits result`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onAutoFillRequest()
            awaitItemMatching { it.showAutoFillConfirm }
            viewModel.events.test {
                viewModel.onAutoFill()

                val event = awaitItem() as CalendarEvent.AutoFillCompleted
                assertEquals(22, event.result.filledDays)
                assertEquals(0, event.result.unfilledDays)
            }

            val state = awaitItemMatching { it.days.size == 23 && !it.isAutoFilling }
            assertFalse(state.showAutoFillConfirm)
            assertEquals(todayPlan.toPlannedLunch(), state.selectedDay)
            assertTrue(repository.saved.all { YearMonth.from(it.date) == YearMonth.of(2026, 10) && !it.date.isBefore(today) })
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

            assertEquals(CalendarEvent.AutoFillCompleted(AutoFillResult(0, 0, 0, 22), PlanView.LUNCH), awaitItem())
        }
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `switching to dinners shows the dinner of the selected day`() = runTest {
        viewModel.uiState.test {
            val lunchState = awaitItemMatching { !it.isLoading }
            assertEquals(PlanView.LUNCH, lunchState.view)

            viewModel.onViewChange(PlanView.DINNER)

            val state = awaitItemMatching { it.view == PlanView.DINNER }
            assertEquals(todayDinner.toPlannedDinner(), state.selectedDinner)
            assertTrue(state.isPlanned(today))
            assertFalse(state.isPlanned(today.plusDays(1)))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `autofill in the dinner view fills only dinners`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }
            viewModel.onViewChange(PlanView.DINNER)
            awaitItemMatching { it.view == PlanView.DINNER }

            viewModel.events.test {
                viewModel.onAutoFill()

                val event = awaitItem() as CalendarEvent.AutoFillCompleted
                assertEquals(PlanView.DINNER, event.view)
                assertEquals(22, event.result.filledDays)
            }

            awaitItemMatching { it.dinners.size == 23 && !it.isAutoFilling }
            assertTrue(repository.saved.isEmpty())
            assertTrue(dinnerRepository.saved.all { YearMonth.from(it.date) == YearMonth.of(2026, 10) })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selection starts empty and tap toggles any day`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onSelectionStart()
            viewModel.onSelectionToggle(today)
            viewModel.onSelectionToggle(today.plusDays(1))

            val state = awaitItemMatching { it.selection?.dates == setOf(today, today.plusDays(1)) }
            assertTrue(state.isSelecting)
            assertTrue(state.isMarked(today.plusDays(1)))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `long press drag enters selection and marks the range`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onSelectionDragStart(today)
            viewModel.onSelectionDragMove(today.plusDays(3))
            viewModel.onSelectionDragEnd()

            awaitItemMatching { it.selection?.dates == (0L..3L).map(today::plusDays).toSet() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `select all marks the whole month and cancel leaves selection mode`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onSelectionStart()
            viewModel.onSelectAllInMonth()
            awaitItemMatching { it.selection?.count == 31 }
            viewModel.onSelectionCancel()

            awaitItemMatching { !it.isSelecting }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `changing view keeps the marked days`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }
            viewModel.onSelectionStart()
            viewModel.onSelectionToggle(today)
            awaitItemMatching { it.selection?.count == 1 }

            viewModel.onViewChange(PlanView.DINNER)

            val state = awaitItemMatching { it.view == PlanView.DINNER }
            assertEquals(setOf(today), state.selection?.dates)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleting selected lunches removes only planned ones, leaves dinners and can be undone`() = runTest {
        repository.saveAll(listOf(nextPlan))
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }
            viewModel.onSelectionDragStart(today)
            viewModel.onSelectionDragMove(nextPlan.date)
            viewModel.onSelectionDragEnd()
            awaitItemMatching { it.selection?.count == 3 }

            lateinit var deletion: PlanDeletion
            viewModel.events.test {
                viewModel.onDeleteSelected()

                deletion = (awaitItem() as CalendarEvent.DaysDeleted).deletion
            }
            assertEquals(PlanView.LUNCH, deletion.view)
            assertEquals(2, deletion.count)

            val cleared = awaitItemMatching { it.days.isEmpty() && !it.isLoading }
            assertFalse(cleared.isSelecting)
            assertEquals(setOf(today), dinnerRepository.current.keys)

            viewModel.onUndoDelete(deletion)

            awaitItemMatching { it.days.keys == setOf(today, nextPlan.date) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleting days without plan reports nothing planned`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }
            viewModel.onSelectionStart()
            viewModel.onSelectionToggle(today.plusDays(10))
            awaitItemMatching { it.selection?.count == 1 }

            viewModel.events.test {
                viewModel.onDeleteSelected()

                assertEquals(CalendarEvent.NothingPlanned, awaitItem())
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleting in the dinner view removes only dinners`() = runTest {
        repository.saveAll(listOf(nextPlan))
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }
            viewModel.onViewChange(PlanView.DINNER)
            awaitItemMatching { it.view == PlanView.DINNER }
            viewModel.onSelectionStart()
            viewModel.onSelectionToggle(today)
            awaitItemMatching { it.selection?.count == 1 }

            viewModel.events.test {
                viewModel.onDeleteSelected()

                assertEquals(PlanView.DINNER, (awaitItem() as CalendarEvent.DaysDeleted).deletion.view)
            }

            awaitItemMatching { it.dinners.isEmpty() }
            assertEquals(setOf(today, nextPlan.date, novemberPlan.date), repository.current.keys)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filling the selection fills only its empty days`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }
            viewModel.onSelectionDragStart(today)
            viewModel.onSelectionDragMove(today.plusDays(2))
            viewModel.onSelectionDragEnd()
            awaitItemMatching { it.selection?.count == 3 }

            viewModel.events.test {
                viewModel.onFillSelected()

                val event = awaitItem() as CalendarEvent.SelectionFilled
                assertEquals(2, event.result.filledDays)
            }

            val state = awaitItemMatching { it.days.size == 3 && !it.isAutoFilling }
            assertFalse(state.isSelecting)
            assertEquals(todayPlan.toPlannedLunch(), state.days[today])
            assertEquals(setOf(today.plusDays(1), today.plusDays(2)), repository.saved.map { it.date }.toSet())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `copying and pasting keeps the gaps and can be undone`() = runTest {
        repository.saveAll(listOf(nextPlan))
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }
            viewModel.onSelectionDragStart(today)
            viewModel.onSelectionDragMove(nextPlan.date)
            viewModel.onSelectionDragEnd()
            awaitItemMatching { it.selection?.count == 3 }

            viewModel.onCopySelected()
            val pasting = awaitItemMatching { it.isPasting }
            assertFalse(pasting.isSelecting)
            assertEquals(3, pasting.clipboard?.count)

            val target = today.plusDays(7)
            lateinit var paste: PlanPaste
            viewModel.events.test {
                viewModel.onPasteAt(target)

                paste = (awaitItem() as CalendarEvent.DaysPasted).paste
            }
            assertEquals(2, paste.count)

            val pasted = awaitItemMatching { it.days.size == 4 && !it.isPasting }
            assertEquals(todayPlan.toPlannedLunch().slots, pasted.days[target]?.slots)
            assertEquals(nextPlan.toPlannedLunch().slots, pasted.days[target.plusDays(2)]?.slots)
            assertNull(pasted.days[target.plusDays(1)])

            viewModel.onUndoPaste(paste)

            awaitItemMatching { it.days.keys == setOf(today, nextPlan.date) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancelling a paste leaves the plan untouched`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }
            viewModel.onSelectionStart()
            viewModel.onSelectionToggle(today)
            viewModel.onCopySelected()
            awaitItemMatching { it.isPasting }

            viewModel.onPasteCancel()

            awaitItemMatching { !it.isPasting && !it.isSelecting }
            assertTrue(repository.saved.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
