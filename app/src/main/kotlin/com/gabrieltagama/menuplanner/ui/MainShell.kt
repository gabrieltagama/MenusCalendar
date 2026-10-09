package com.gabrieltagama.menuplanner.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.gabrieltagama.menuplanner.feature.calendar.navigation.navigateToShoppingList
import com.gabrieltagama.menuplanner.feature.dishes.navigation.dishesGraph
import com.gabrieltagama.menuplanner.feature.dishes.navigation.navigateToDishEditor
import com.gabrieltagama.menuplanner.share.ShareEvent
import com.gabrieltagama.menuplanner.share.ShareViewModel
import com.gabrieltagama.menuplanner.share.text

/**
 * Authenticated shell: a Scaffold with the bottom NavigationBar (Calendario, Platos, Más), the
 * snackbar host and the NavHost of the feature graphs. Feature screens draw their own top bar,
 * so the shell only reserves space for the navigation bar. "Más" opens the share/logout sheet.
 */
@Composable
internal fun MainShell(
    onLogout: () -> Unit,
    shareViewModel: ShareViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val shareState by shareViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val chooserTitle = stringResource(R.string.share_chooser_title)
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(shareViewModel::onImport)
    }

    LaunchedEffect(shareViewModel) {
        shareViewModel.events.collect { event ->
            when (event) {
                is ShareEvent.ShareFile -> context.shareJson(event.uri, chooserTitle)
            }
        }
    }

    val messageText = shareState.message?.text()
    LaunchedEffect(shareState.message) {
        if (messageText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(messageText)
        shareViewModel.onMessageShown()
    }

    MainShellContent(
        snackbarHostState = snackbarHostState,
        isBusy = shareState.isBusy,
        onExport = shareViewModel::onExport,
        onImport = { importLauncher.launch(ImportMimeTypes) },
        onLogout = onLogout
    )
}

@Composable
private fun MainShellContent(
    snackbarHostState: SnackbarHostState,
    isBusy: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onLogout: () -> Unit,
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
                onOpenShoppingList = { navController.navigateToShoppingList(it) },
                onBack = { navController.popBackStack() }
            )
            dishesGraph(onOpenDish = { navController.navigateToDishEditor(it) }, onBack = { navController.popBackStack() })
        }
    }

    if (showMoreSheet)
        MoreActionsSheet(
            isBusy = isBusy,
            onDismiss = { showMoreSheet = false },
            onExport = onExport,
            onImport = onImport,
            onLogout = onLogout
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

private fun Context.shareJson(uri: Uri, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = JsonMimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(send, chooserTitle))
}

@Preview(showBackground = true)
@Composable
private fun MenuPlannerNavigationBarPreview() = MenuPlannerTheme {
    MenuPlannerNavigationBar(currentDestination = null, onSelect = {}, onMore = {})
}

private val ImportMimeTypes = arrayOf("application/json", "text/plain", "application/octet-stream")
private const val JsonMimeType = "application/json"
