package com.gabrieltagama.menuplanner.core.data.share

import com.gabrieltagama.menuplanner.core.data.database.entity.DinnerDayEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity
import com.gabrieltagama.menuplanner.core.data.share.dto.DinnerDayDto
import com.gabrieltagama.menuplanner.core.data.share.dto.DishDto
import com.gabrieltagama.menuplanner.core.data.share.dto.IngredientDto
import com.gabrieltagama.menuplanner.core.data.share.dto.MealDayDto
import com.gabrieltagama.menuplanner.core.data.share.dto.ShareDocumentDto
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.ImportContent
import com.gabrieltagama.menuplanner.core.domain.model.ImportedDinner
import com.gabrieltagama.menuplanner.core.domain.model.ImportedLunch
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate

/**
 * Pure conversion between the share DTOs and the model. Unknown enum names, malformed dates or
 * dishes breaking the SaveDishUseCase rules (blank name, blank ingredient, negative or
 * non-finite quantity) make the whole file invalid (InvalidImportFile); calendar rules are checked
 * later by the import, once the dishes are known. Repeated dish ids, lunch dates or dinner dates
 * keep only their newest version. Dates use ISO-8601 (yyyy-MM-dd and instants).
 */
internal object ShareDocumentMapper {

    fun toDocument(
        dishes: List<Dish>,
        mealDays: List<MealDayEntity>,
        exportedAt: Instant,
        dinnerDays: List<DinnerDayEntity> = emptyList()
    ): ShareDocumentDto =
        ShareDocumentDto(
            exportedAt = exportedAt.toString(),
            dishes = dishes.map { it.toDto() },
            mealDays = mealDays.map { it.toDto() },
            dinnerDays = dinnerDays.map { it.toDto() }
        )

    fun toImportContent(document: ShareDocumentDto): Outcome<ImportContent> =
        try {
            Outcome.Success(
                ImportContent(
                    dishes = document.dishes.map { it.toDomain() }.newestPerId(),
                    lunches = document.mealDays.map { it.toImported() }.newestPer(ImportedLunch::date, ImportedLunch::updatedAt),
                    dinners = document.dinnerDays.map { it.toImported() }.newestPer(ImportedDinner::date, ImportedDinner::updatedAt)
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
        rating = rating.name,
        mealTime = mealTime.name,
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

    private fun DinnerDayEntity.toDto(): DinnerDayDto = DinnerDayDto(
        date = LocalDate.ofEpochDay(date).toString(),
        dishId = dishId,
        updatedAt = Instant.ofEpochMilli(updatedAt).toString()
    )

    private fun List<Dish>.newestPerId(): List<Dish> = newestPer(Dish::id, Dish::updatedAt)

    private fun <T, K> List<T>.newestPer(key: (T) -> K, updatedAt: (T) -> Instant): List<T> =
        groupBy(key).values.map { versions -> versions.maxBy(updatedAt) }

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
        rating = Rating.valueOf(rating),
        mealTime = MealTime.valueOf(mealTime),
        updatedAt = Instant.parse(updatedAt)
    )

    private fun MealDayDto.toImported(): ImportedLunch = ImportedLunch(
        date = LocalDate.parse(date),
        dishIds = listOfNotNull(
            starterId?.let { MenuSlot.STARTER to it },
            mainId?.let { MenuSlot.MAIN to it },
            singleId?.let { MenuSlot.SINGLE to it },
            dessertId?.let { MenuSlot.DESSERT to it }
        ).toMap(),
        updatedAt = Instant.parse(updatedAt)
    )

    private fun DinnerDayDto.toImported(): ImportedDinner =
        ImportedDinner(date = LocalDate.parse(date), dishId = dishId, updatedAt = Instant.parse(updatedAt))
}
