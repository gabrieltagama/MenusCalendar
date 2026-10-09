package com.gabrieltagama.menuplanner.feature.dishes.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gabrieltagama.menuplanner.feature.dishes.editor.DishEditorScreenRoute
import com.gabrieltagama.menuplanner.feature.dishes.list.DishListScreenRoute
import kotlinx.serialization.Serializable

/**
 * Type-safe routes and navigation graph of the recipe book feature.
 */
@Serializable
data object DishListRoute

@Serializable
data class DishEditorRoute(val dishId: String? = null)

fun NavGraphBuilder.dishesGraph(onOpenDish: (String?) -> Unit, onBack: () -> Unit) {
    composable<DishListRoute> { DishListScreenRoute(onOpenDish = onOpenDish) }
    composable<DishEditorRoute> { DishEditorScreenRoute(onBack = onBack) }
}

fun NavController.navigateToDishEditor(dishId: String?) = navigate(DishEditorRoute(dishId))
