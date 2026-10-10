package com.gabrieltagama.menuplanner.core.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.ui.R

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

@Composable
fun DomainError.message(): String = when (this) {
    DomainError.BlankDishName -> stringResource(R.string.error_blank_dish_name)
    is DomainError.InvalidIngredient -> stringResource(R.string.error_invalid_ingredient, index + 1)
    DomainError.DishNotFound -> stringResource(R.string.error_dish_not_found)
    DomainError.DishInUse -> stringResource(R.string.error_dish_in_use)
    is DomainError.WrongDishType -> stringResource(R.string.error_wrong_dish_type, expected.label())
    DomainError.InvalidImportFile -> stringResource(R.string.error_invalid_import_file)
    DomainError.UnsupportedImportVersion -> stringResource(R.string.error_unsupported_import_version)
    DomainError.CloudUnavailable -> stringResource(R.string.error_cloud_unavailable)
    DomainError.CloudAuthorizationRequired -> stringResource(R.string.error_cloud_authorization_required)
}
