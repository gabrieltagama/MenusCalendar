package com.gabrieltagama.menuplanner.feature.calendar.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gabrieltagama.menuplanner.feature.calendar.dinner.DinnerEditorScreenRoute
import com.gabrieltagama.menuplanner.feature.calendar.lunch.LunchEditorScreenRoute
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
data class LunchEditorRoute(val epochDay: Long) {
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)
}

@Serializable
data class DinnerEditorRoute(val epochDay: Long) {
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)
}

@Serializable
data class ShoppingListRoute(val year: Int, val month: Int) {
    val yearMonth: YearMonth get() = YearMonth.of(year, month)
}

fun NavGraphBuilder.calendarGraph(
    onOpenLunch: (LocalDate) -> Unit,
    onOpenDinner: (LocalDate) -> Unit,
    onOpenShoppingList: (YearMonth) -> Unit,
    onBack: () -> Unit
) {
    composable<CalendarRoute> {
        CalendarScreenRoute(onOpenLunch = onOpenLunch, onOpenDinner = onOpenDinner, onOpenShoppingList = onOpenShoppingList)
    }
    composable<LunchEditorRoute> { LunchEditorScreenRoute(onBack = onBack) }
    composable<DinnerEditorRoute> { DinnerEditorScreenRoute(onBack = onBack) }
    composable<ShoppingListRoute> { ShoppingListScreenRoute(onBack = onBack) }
}

fun NavController.navigateToLunchEditor(date: LocalDate) = navigate(LunchEditorRoute(date.toEpochDay()))

fun NavController.navigateToDinnerEditor(date: LocalDate) = navigate(DinnerEditorRoute(date.toEpochDay()))

fun NavController.navigateToShoppingList(month: YearMonth) = navigate(ShoppingListRoute(month.year, month.monthValue))
