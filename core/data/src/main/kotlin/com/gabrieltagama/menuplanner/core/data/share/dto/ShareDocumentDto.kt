package com.gabrieltagama.menuplanner.core.data.share.dto

import kotlinx.serialization.Serializable

/**
 * Wire format of a shared JSON file. Fields are only ever added (with defaults) so older files
 * keep parsing; a breaking change bumps schemaVersion.
 */
@Serializable
internal data class ShareDocumentDto(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val app: String = APP_NAME,
    val exportedAt: String,
    val dishes: List<DishDto>,
    val mealDays: List<MealDayDto>
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val APP_NAME = "MenuPlanner"
    }
}

@Serializable
internal data class DishDto(
    val id: String,
    val name: String,
    val description: String,
    val ingredients: List<IngredientDto>,
    val preparation: String,
    val type: String,
    val heaviness: String,
    val updatedAt: String
)

@Serializable
internal data class IngredientDto(
    val name: String,
    val quantity: Double,
    val unit: String
)

@Serializable
internal data class MealDayDto(
    val date: String,
    val starterId: String? = null,
    val mainId: String? = null,
    val singleId: String? = null,
    val dessertId: String? = null,
    val updatedAt: String
)
