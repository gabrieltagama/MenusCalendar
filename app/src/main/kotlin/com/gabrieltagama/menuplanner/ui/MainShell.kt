package com.gabrieltagama.menuplanner.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gabrieltagama.menuplanner.R
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.calendar.navigation.CalendarRoute
import com.gabrieltagama.menuplanner.feature.calendar.navigation.calendarGraph
import com.gabrieltagama.menuplanner.feature.calendar.navigation.navigateToDayEditor
import com.gabrieltagama.menuplanner.feature.calendar.navigation.navigateToDinnerEditor
import com.gabrieltagama.menuplanner.feature.calendar.navigation.navigateToShoppingList
import com.gabrieltagama.menuplanner.feature.dishes.navigation.dishesGraph
import com.gabrieltagama.menuplanner.feature.dishes.navigation.navigateToDishEditor
import com.gabrieltagama.menuplanner.feature.settings.navigation.settingsGraph
import com.gabrieltagama.menuplanner.feature.share.MoreActionsSheet
import com.gabrieltagama.menuplanner.feature.share.rememberShareController

/**
 * Main shell: a Scaffold with the bottom NavigationBar (Calendario, Platos, Ajustes, Más), the
 * snackbar host and the NavHost of the feature graphs. Feature screens draw their own top bar,
 * so the shell only reserves space for the navigation bar. "Más" opens the share actions of
 * :feature:share.
 */
@Composable
internal fun MainShell() {
    val snackbarHostState = remember { SnackbarHostState() }
    val share = rememberShareController(snackbarHostState)
    MainShellContent(
        snackbarHostState = snackbarHostState,
        isBusy = share.isBusy,
        onExport = share.onExport,
        onImport = share.onImport
    )
}

@Composable
private fun MainShellContent(
    snackbarHostState: SnackbarHostState,
    isBusy: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    var showMoreSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentDestination.isTopLevel())
                MenuPlannerNavigationBar(
                    currentDestination = currentDestination,
                    onSelect = navController::navigateToTopLevel,
                    onMore = { showMoreSheet = true }
                )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CalendarRoute,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            calendarGraph(
                onOpenDay = { navController.navigateToDayEditor(it) },
                onOpenDinner = { navController.navigateToDinnerEditor(it) },
                onOpenShoppingList = { navController.navigateToShoppingList(it) },
                onBack = { navController.popBackStack() }
            )
            dishesGraph(onOpenDish = { navController.navigateToDishEditor(it) }, onBack = { navController.popBackStack() })
            settingsGraph()
        }
    }

    if (showMoreSheet)
        MoreActionsSheet(
            isBusy = isBusy,
            onDismiss = { showMoreSheet = false },
            onExport = onExport,
            onImport = onImport
        )
}

@Composable
private fun MenuPlannerNavigationBar(
    currentDestination: NavDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    onMore: () -> Unit
) = NavigationBar {
    TopLevelDestination.entries.forEach { destination ->
        val label = stringResource(destination.labelRes)
        NavigationBarItem(
            selected = destination.isSelectedIn(currentDestination),
            onClick = { onSelect(destination) },
            icon = { Icon(destination.icon, contentDescription = null) },
            label = { Text(label) }
        )
    }
    NavigationBarItem(
        selected = false,
        onClick = onMore,
        icon = { Icon(Icons.Filled.MoreHoriz, contentDescription = null) },
        label = { Text(stringResource(R.string.nav_more)) }
    )
}

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) =
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

@Preview(showBackground = true)
@Composable
private fun MenuPlannerNavigationBarPreview() = MenuPlannerTheme {
    MenuPlannerNavigationBar(currentDestination = null, onSelect = {}, onMore = {})
}
