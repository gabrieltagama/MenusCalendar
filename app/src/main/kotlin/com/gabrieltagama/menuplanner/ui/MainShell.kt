package com.gabrieltagama.menuplanner.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.calendar.navigation.CalendarRoute
import com.gabrieltagama.menuplanner.feature.calendar.navigation.calendarGraph
import com.gabrieltagama.menuplanner.feature.calendar.navigation.navigateToDayEditor
import com.gabrieltagama.menuplanner.feature.calendar.navigation.navigateToDinnerEditor
import com.gabrieltagama.menuplanner.feature.calendar.navigation.navigateToShoppingList
import com.gabrieltagama.menuplanner.feature.dishes.navigation.dishesGraph
import com.gabrieltagama.menuplanner.feature.dishes.navigation.navigateToDishEditor
import com.gabrieltagama.menuplanner.feature.settings.navigation.settingsGraph
import com.gabrieltagama.menuplanner.feature.settings.ui.FileShareActions
import com.gabrieltagama.menuplanner.feature.share.rememberShareController

/**
 * Main shell: a Scaffold with the bottom NavigationBar (Calendario, Platos, Ajustes) and the NavHost
 * of the feature graphs. Feature screens draw their own top bar, so the shell
 * only reserves space for the navigation bar. Ajustes shows the export and import of :feature:share
 * through rememberFileShare.
 */
@Composable
internal fun MainShell(navController: NavHostController = rememberNavController()) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentDestination.isTopLevel())
                MenuPlannerNavigationBar(currentDestination = currentDestination, onSelect = navController::navigateToTopLevel)
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
            settingsGraph(rememberFileShare = { rememberFileShare(it) })
        }
    }
}

@Composable
private fun rememberFileShare(snackbarHostState: SnackbarHostState): FileShareActions =
    rememberShareController(snackbarHostState).let { FileShareActions(isBusy = it.isBusy, onExport = it.onExport, onImport = it.onImport) }

@Composable
private fun MenuPlannerNavigationBar(
    currentDestination: NavDestination?,
    onSelect: (TopLevelDestination) -> Unit
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
    MenuPlannerNavigationBar(currentDestination = null, onSelect = {})
}
