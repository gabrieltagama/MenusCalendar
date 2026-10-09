package com.gabrieltagama.menuplanner.feature.calendar.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gabrieltagama.menuplanner.feature.calendar.day.DayEditorScreenRoute
import com.gabrieltagama.menuplanner.feature.calendar.month.CalendarScreenRoute
import java.time.LocalDate
import kotlinx.serialization.Serializable

/**
 * Type-safe routes and navigation graph of the monthly calendar feature. The day is passed as
 * an epoch day so the route only carries primitive arguments.
 */
@Serializable
data object CalendarRoute

@Serializable
data class DayEditorRoute(val epochDay: Long)

fun NavGraphBuilder.calendarGraph(onOpenDay: (LocalDate) -> Unit, onBack: () -> Unit) {
    composable<CalendarRoute> { CalendarScreenRoute(onOpenDay = onOpenDay) }
    composable<DayEditorRoute> { DayEditorScreenRoute(onBack = onBack) }
}

fun NavController.navigateToDayEditor(date: LocalDate) = navigate(DayEditorRoute(date.toEpochDay()))
