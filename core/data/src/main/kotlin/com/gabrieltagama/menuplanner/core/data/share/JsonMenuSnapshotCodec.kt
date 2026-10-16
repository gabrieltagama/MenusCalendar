package com.gabrieltagama.menuplanner.core.data.share

import com.gabrieltagama.menuplanner.core.data.share.dto.DinnerDayDto
import com.gabrieltagama.menuplanner.core.data.share.dto.DishDto
import com.gabrieltagama.menuplanner.core.data.share.dto.IngredientDto
import com.gabrieltagama.menuplanner.core.data.share.dto.MealDayDto
import com.gabrieltagama.menuplanner.core.data.share.dto.ShareDocumentDto
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import com.gabrieltagama.menuplanner.core.domain.model.share.DinnerEntry
import com.gabrieltagama.menuplanner.core.domain.model.share.LunchEntry
import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import com.gabrieltagama.menuplanner.core.domain.repository.MenuSnapshotCodec
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * JSON format of shared files and backups (ShareDocumentDto). The schema version is read before
 * full decoding so a newer file reports UnsupportedImportVersion instead of a parse error. Malformed
 * JSON, unknown enum names, malformed dates or dishes breaking the Dish rules make the whole file
 * invalid (InvalidImportFile); SerializationException and NumberFormatException extend
 * IllegalArgumentException, so one catch covers them. Repeated dish ids, lunch dates or dinner dates
 * keep only their newest version.
 */
internal class JsonMenuSnapshotCodec @Inject constructor(private val json: Json) : MenuSnapshotCodec {

    override fun encode(snapshot: MenuSnapshot, exportedAt: Instant): String =
        json.encodeToString(ShareDocumentDto.serializer(), snapshot.toDto(exportedAt))

    override fun decode(text: String): Outcome<MenuSnapshot> =
        try {
            if (schemaVersionOf(text) > ShareDocumentDto.CURRENT_SCHEMA_VERSION) Outcome.Failure(DomainError.UnsupportedImportVersion)
            else Outcome.Success(json.decodeFromString(ShareDocumentDto.serializer(), text).toSnapshot())
        } catch (exception: IllegalArgumentException) {
            Outcome.Failure(DomainError.InvalidImportFile)
        } catch (exception: DateTimeException) {
            Outcome.Failure(DomainError.InvalidImportFile)
        }

    private fun schemaVersionOf(text: String): Int =
        json.parseToJsonElement(text).jsonObject[SCHEMA_VERSION_KEY]?.jsonPrimitive?.int ?: ShareDocumentDto.CURRENT_SCHEMA_VERSION

    private fun MenuSnapshot.toDto(exportedAt: Instant): ShareDocumentDto = ShareDocumentDto(
        exportedAt = exportedAt.toString(),
        dishes = dishes.map { it.toDto() },
        mealDays = lunches.map { it.toDto() },
        dinnerDays = dinners.map { it.toDto() }
    )

    private fun ShareDocumentDto.toSnapshot(): MenuSnapshot = MenuSnapshot(
        dishes = dishes.map { it.toDomain() }.newestBy(Dish::id, Dish::updatedAt),
        lunches = mealDays.map { it.toDomain() }.newestBy(LunchEntry::date, LunchEntry::updatedAt),
        dinners = dinnerDays.map { it.toDomain() }.newestBy(DinnerEntry::date, DinnerEntry::updatedAt)
    )

    private fun Dish.toDto(): DishDto = DishDto(
        id = id.value,
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

    private fun LunchEntry.toDto(): MealDayDto = MealDayDto(
        date = date.toString(),
        starterId = starterId?.value,
        mainId = mainId?.value,
        singleId = singleId?.value,
        updatedAt = updatedAt.toString()
    )

    private fun DinnerEntry.toDto(): DinnerDayDto = DinnerDayDto(date = date.toString(), dishId = dishId.value, updatedAt = updatedAt.toString())

    private fun DishDto.toDomain(): Dish {
        require(id.isNotBlank())
        val dish = Dish(
            id = DishId(id),
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
        require(dish.validationError == null)
        return dish
    }

    private fun MealDayDto.toDomain(): LunchEntry = LunchEntry(
        date = LocalDate.parse(date),
        starterId = starterId?.let(::DishId),
        mainId = mainId?.let(::DishId),
        singleId = singleId?.let(::DishId),
        updatedAt = Instant.parse(updatedAt)
    )

    private fun DinnerDayDto.toDomain(): DinnerEntry = DinnerEntry(LocalDate.parse(date), DishId(dishId), Instant.parse(updatedAt))

    private fun <T, K> List<T>.newestBy(key: (T) -> K, updatedAt: (T) -> Instant): List<T> =
        groupBy(key).values.map { versions -> versions.maxBy(updatedAt) }

    private companion object {
        const val SCHEMA_VERSION_KEY = "schemaVersion"
    }
}
