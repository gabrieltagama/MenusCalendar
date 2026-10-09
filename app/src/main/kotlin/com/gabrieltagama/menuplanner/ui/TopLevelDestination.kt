package com.gabrieltagama.menuplanner.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.gabrieltagama.menuplanner.R
import com.gabrieltagama.menuplanner.feature.calendar.navigation.CalendarRoute
import com.gabrieltagama.menuplanner.feature.dishes.navigation.DishListRoute

/**
 * Top-level tabs of the main shell, each bound to the start route of a feature graph.
 */
internal enum class TopLevelDestination(
    val route: Any,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
) {
    CALENDAR(CalendarRoute, R.string.nav_calendar, Icons.Filled.CalendarMonth),
    DISHES(DishListRoute, R.string.nav_dishes, Icons.Filled.RestaurantMenu);

    fun matches(destination: NavDestination): Boolean = when (this) {
        CALENDAR -> destination.hasRoute<CalendarRoute>()
        DISHES -> destination.hasRoute<DishListRoute>()
    }

    fun isSelectedIn(destination: NavDestination?): Boolean =
        destination?.hierarchy?.any(::matches) == true
}

internal fun NavDestination?.isTopLevel(): Boolean =
    this != null && TopLevelDestination.entries.any { it.matches(this) }
