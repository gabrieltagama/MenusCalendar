package com.gabrieltagama.menuplanner.core.data.share

import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity
import com.gabrieltagama.menuplanner.core.data.share.dto.DishDto
import com.gabrieltagama.menuplanner.core.data.share.dto.IngredientDto
import com.gabrieltagama.menuplanner.core.data.share.dto.MealDayDto
import com.gabrieltagama.menuplanner.core.data.share.dto.ShareDocumentDto
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate

/**
 * Pure conversion between the share DTOs and the model. Unknown enum names, malformed dates or
 * dishes breaking the SaveDishUseCase rules (blank name, blank ingredient, negative or
 * non-finite quantity) make the whole file invalid (InvalidImportFile). Repeated dish ids or
 * meal day dates keep only their newest version. Dates use ISO-8601 (yyyy-MM-dd and instants).
 */
internal object ShareDocumentMapper {

    fun toDocument(dishes: List<Dish>, mealDays: List<MealDayEntity>, exportedAt: Instant): ShareDocumentDto =
        ShareDocumentDto(
            exportedAt = exportedAt.toString(),
            dishes = dishes.map { it.toDto() },
            mealDays = mealDays.map { it.toDto() }
        )

    fun toImportContent(document: ShareDocumentDto): Outcome<ImportContent> =
        try {
            Outcome.Success(
                ImportContent(
                    dishes = document.dishes.map { it.toDomain() }.newestPerId(),
                    mealDays = document.mealDays.map { it.toEntity() }.newestPerDate()
                )
            )
        } catch (exception: IllegalArgumentException) {
            Outcome.Failure(DomainError.InvalidImportFile)
        } catch (exception: DateTimeException) {
            Outcome.Failure(DomainError.InvalidImportFile)
        }

    private fun Dish.toDto(): DishDto = DishDto(
        id = id,
        name = name,
        description = description,
        ingredients = ingredients.map { IngredientDto(it.name, it.quantity, it.unit.name) },
        preparation = preparation,
        type = type.name,
        heaviness = heaviness.name,
        updatedAt = updatedAt.toString()
    )

    private fun MealDayEntity.toDto(): MealDayDto = MealDayDto(
        date = LocalDate.ofEpochDay(date).toString(),
        starterId = starterId,
        mainId = mainId,
        singleId = singleId,
        dessertId = dessertId,
        updatedAt = Instant.ofEpochMilli(updatedAt).toString()
    )

    private fun List<Dish>.newestPerId(): List<Dish> =
        groupBy(Dish::id).values.map { versions -> versions.maxBy(Dish::updatedAt) }

    private fun List<MealDayEntity>.newestPerDate(): List<MealDayEntity> =
        groupBy(MealDayEntity::date).values.map { versions -> versions.maxBy(MealDayEntity::updatedAt) }

    private fun DishDto.toDomain(): Dish {
        require(id.isNotBlank() && name.isNotBlank())
        require(ingredients.all { it.isValid() })
        return toDish()
    }

    private fun IngredientDto.isValid(): Boolean = name.isNotBlank() && quantity.isFinite() && quantity >= 0.0

    private fun DishDto.toDish(): Dish = Dish(
        id = id,
        name = name,
        description = description,
        ingredients = ingredients.map { Ingredient(it.name, it.quantity, MeasureUnit.valueOf(it.unit)) },
        preparation = preparation,
        type = DishType.valueOf(type),
        heaviness = Heaviness.valueOf(heaviness),
        updatedAt = Instant.parse(updatedAt)
    )

    private fun MealDayDto.toEntity(): MealDayEntity = MealDayEntity(
        date = LocalDate.parse(date).toEpochDay(),
        starterId = starterId,
        mainId = mainId,
        singleId = singleId,
        dessertId = dessertId,
        updatedAt = Instant.parse(updatedAt).toEpochMilli()
    )
}
