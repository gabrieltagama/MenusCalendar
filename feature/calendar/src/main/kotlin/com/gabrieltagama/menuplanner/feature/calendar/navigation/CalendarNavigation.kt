package com.gabrieltagama.menuplanner.feature.calendar.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gabrieltagama.menuplanner.feature.calendar.day.DayEditorScreenRoute
import com.gabrieltagama.menuplanner.feature.calendar.month.CalendarScreenRoute
import com.gabrieltagama.menuplanner.feature.calendar.shopping.ShoppingListScreenRoute
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.serialization.Serializable

/**
 * Type-safe routes and navigation graph of the monthly calendar feature. The day is passed as
 * an epoch day and the month as year + month number, so routes only carry primitive arguments.
 */
@Serializable
data object CalendarRoute

@Serializable
data class DayEditorRoute(val epochDay: Long)

@Serializable
data class ShoppingListRoute(val year: Int, val month: Int) {
    val yearMonth: YearMonth get() = YearMonth.of(year, month)
}

fun NavGraphBuilder.calendarGraph(
    onOpenDay: (LocalDate) -> Unit,
    onOpenShoppingList: (YearMonth) -> Unit,
    onBack: () -> Unit
) {
    composable<CalendarRoute> { CalendarScreenRoute(onOpenDay = onOpenDay, onOpenShoppingList = onOpenShoppingList) }
    composable<DayEditorRoute> { DayEditorScreenRoute(onBack = onBack) }
    composable<ShoppingListRoute> { ShoppingListScreenRoute(onBack = onBack) }
}

fun NavController.navigateToDayEditor(date: LocalDate) = navigate(DayEditorRoute(date.toEpochDay()))

fun NavController.navigateToShoppingList(month: YearMonth) = navigate(ShoppingListRoute(month.year, month.monthValue))
