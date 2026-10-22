package com.gabrieltagama.menuplanner.core.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.gabrieltagama.menuplanner.core.domain.common.CloudError
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.PlanningError
import com.gabrieltagama.menuplanner.core.domain.common.RecipeError
import com.gabrieltagama.menuplanner.core.domain.common.ShareError
import com.gabrieltagama.menuplanner.core.domain.common.StorageError
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import com.gabrieltagama.menuplanner.core.domain.text.spanishLocale
import com.gabrieltagama.menuplanner.core.ui.R
import java.time.format.DateTimeFormatter

/**
 * Localized labels for domain enums and errors, shared by every feature.
 */
@Composable
fun DishType.label(): String = stringResource(
    when (this) {
        DishType.STARTER -> R.string.dish_type_starter
        DishType.MAIN -> R.string.dish_type_main
        DishType.DESSERT -> R.string.dish_type_dessert
        DishType.SINGLE -> R.string.dish_type_single
    }
)

@Composable
fun Heaviness.label(): String = stringResource(
    when (this) {
        Heaviness.VERY_LOW -> R.string.heaviness_very_low
        Heaviness.MEDIUM -> R.string.heaviness_medium
        Heaviness.VERY_HIGH -> R.string.heaviness_very_high
    }
)

@Composable
fun MealTime.label(): String = stringResource(
    when (this) {
        MealTime.LUNCH -> R.string.meal_time_lunch
        MealTime.DINNER -> R.string.meal_time_dinner
        MealTime.ANY -> R.string.meal_time_any
    }
)

@Composable
fun Meal.label(): String = stringResource(
    when (this) {
        Meal.LUNCH -> R.string.meal_time_lunch
        Meal.DINNER -> R.string.meal_time_dinner
    }
)

fun Rating.label(): String = FILLED_STAR.repeat(stars) + EMPTY_STAR.repeat(Rating.entries.size - stars)

private const val FILLED_STAR = "★"
private const val EMPTY_STAR = "☆"

@Composable
fun MeasureUnit.label(): String = stringResource(
    when (this) {
        MeasureUnit.GRAM -> R.string.unit_gram
        MeasureUnit.KILOGRAM -> R.string.unit_kilogram
        MeasureUnit.MILLILITER -> R.string.unit_milliliter
        MeasureUnit.LITER -> R.string.unit_liter
        MeasureUnit.UNIT -> R.string.unit_unit
        MeasureUnit.TABLESPOON -> R.string.unit_tablespoon
        MeasureUnit.TEASPOON -> R.string.unit_teaspoon
        MeasureUnit.CUP -> R.string.unit_cup
        MeasureUnit.PINCH -> R.string.unit_pinch
        MeasureUnit.TO_TASTE -> R.string.unit_to_taste
    }
)

private val errorDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", spanishLocale)

@Composable
fun DomainError.message(): String = when (this) {
    RecipeError.BlankDishName -> stringResource(R.string.error_blank_dish_name)
    is RecipeError.InvalidIngredient -> stringResource(R.string.error_invalid_ingredient, index + 1)
    RecipeError.DishNotFound -> stringResource(R.string.error_dish_not_found)
    RecipeError.DishInUse -> stringResource(R.string.error_dish_in_use)
    is RecipeError.DishPlannedIncompatibly -> pluralStringResource(R.plurals.error_dish_planned_incompatibly, days, days, firstDate.format(errorDateFormatter))
    is PlanningError.WrongDishType -> stringResource(R.string.error_wrong_dish_type, expected.label())
    PlanningError.NotADinnerDish -> stringResource(R.string.error_not_a_dinner_dish)
    ShareError.InvalidImportFile -> stringResource(R.string.error_invalid_import_file)
    ShareError.UnsupportedImportVersion -> stringResource(R.string.error_unsupported_import_version)
    CloudError.CloudUnavailable -> stringResource(R.string.error_cloud_unavailable)
    CloudError.CloudAuthorizationRequired -> stringResource(R.string.error_cloud_authorization_required)
    CloudError.AuthorizationCancelled -> stringResource(R.string.error_cloud_authorization_cancelled)
    StorageError.StorageUnavailable -> stringResource(R.string.error_storage_unavailable)
}
