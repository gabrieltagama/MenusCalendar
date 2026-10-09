package com.gabrieltagama.menuplanner.feature.calendar.month

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.feature.calendar.R
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Stateless CalendarScreen: weekday headers, planned dish names in the grid and the summary card,
 * and day/navigation callbacks.
 */
@RunWith(AndroidJUnit4::class)
class CalendarScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = LocalDate.of(2026, 10, 9)
    private val menu = DailyMenu.Courses(
        starter = Dish(name = "Gazpacho", type = DishType.STARTER, heaviness = Heaviness.VERY_LOW),
        main = Dish(name = "Pollo asado", type = DishType.MAIN, heaviness = Heaviness.MEDIUM)
    )
    private val state = CalendarUiState(
        month = YearMonth.from(today),
        selectedDate = today,
        today = today,
        isLoading = false,
        days = mapOf(today to MealDay(date = today, menu = menu))
    )

    @Test
    fun rendersWeekdayHeaders() {
        setScreen()

        context.resources.getStringArray(R.array.calendar_weekday_initials).forEach { initial ->
            composeRule.onNodeWithText(initial).assertIsDisplayed()
        }
    }

    @Test
    fun rendersPlannedDayNamesInGridAndSummary() {
        setScreen()

        composeRule.onNodeWithText("Gazpacho · Pollo asado").assertIsDisplayed()
        composeRule.onNodeWithText("Gazpacho").assertIsDisplayed()
        composeRule.onNodeWithText("Pollo asado").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.calendar_edit)).assertIsDisplayed()
    }

    @Test
    fun dayWithoutMenuShowsPlanButton() {
        setScreen(state.copy(selectedDate = today.plusDays(1)))

        composeRule.onNodeWithText(context.getString(R.string.calendar_no_menu)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.calendar_plan)).assertIsDisplayed()
    }

    @Test
    fun clickingDayCellSelectsDate() {
        val selected = mutableListOf<LocalDate>()
        setScreen(onDateSelect = { selected += it })

        composeRule.onNodeWithText("20").performClick()

        assertEquals(listOf(LocalDate.of(2026, 10, 20)), selected)
    }

    @Test
    fun summaryButtonOpensSelectedDay() {
        val opened = mutableListOf<LocalDate>()
        setScreen(onOpenDay = { opened += it })

        composeRule.onNodeWithText(context.getString(R.string.calendar_edit)).performClick()

        assertEquals(listOf(today), opened)
    }

    @Test
    fun todayButtonTriggersCallback() {
        var todayClicks = 0
        setScreen(onToday = { todayClicks++ })

        composeRule.onNodeWithText(context.getString(R.string.calendar_today)).performClick()

        assertEquals(1, todayClicks)
    }

    private fun setScreen(
        screenState: CalendarUiState = state,
        onToday: () -> Unit = {},
        onDateSelect: (LocalDate) -> Unit = {},
        onOpenDay: (LocalDate) -> Unit = {}
    ) = composeRule.setContent {
        MaterialTheme {
            CalendarScreen(
                state = screenState,
                onPreviousMonth = {},
                onNextMonth = {},
                onToday = onToday,
                onDateSelect = onDateSelect,
                onOpenDay = onOpenDay
            )
        }
    }
}
