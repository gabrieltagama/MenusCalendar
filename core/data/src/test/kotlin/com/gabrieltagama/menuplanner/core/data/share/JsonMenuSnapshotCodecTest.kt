package com.gabrieltagama.menuplanner.core.data.share

import com.gabrieltagama.menuplanner.core.data.fake.TestData
import com.gabrieltagama.menuplanner.core.data.share.dto.DishDto
import com.gabrieltagama.menuplanner.core.data.share.dto.IngredientDto
import com.gabrieltagama.menuplanner.core.data.share.dto.MealDayDto
import com.gabrieltagama.menuplanner.core.data.share.dto.ShareDocumentDto
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
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
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests JsonMenuSnapshotCodec: lossless round trip of a snapshot, compatibility with older files
 * (no dinners, desserts in the calendar, no rating or meal time) and the error mapping of malformed,
 * newer or inconsistent files.
 */
class JsonMenuSnapshotCodecTest {
    private val codec = JsonMenuSnapshotCodec(TestData.json)

    private val starter = TestData.dish(
        id = "starter-1",
        name = "Gazpacho",
        type = DishType.STARTER,
        heaviness = Heaviness.VERY_LOW,
        rating = Rating.FOUR,
        mealTime = MealTime.DINNER,
        ingredients = listOf(Ingredient("Tomato", 500.0, MeasureUnit.GRAM), Ingredient("Olive oil", 30.0, MeasureUnit.MILLILITER))
    )
    private val main = TestData.dish(id = "main-1", name = "Paella", type = DishType.MAIN, updatedAt = TestData.NEW_INSTANT)
    private val single = TestData.dish(id = "single-1", name = "Lasagna", type = DishType.SINGLE)
    private val snapshot = MenuSnapshot(
        dishes = listOf(starter, main, single),
        lunches = listOf(
            LunchEntry(LocalDate.of(2026, 3, 10), starterId = starter.id, mainId = main.id, updatedAt = TestData.NEW_INSTANT),
            LunchEntry(LocalDate.of(2026, 3, 11), singleId = single.id, updatedAt = TestData.OLD_INSTANT)
        ),
        dinners = listOf(DinnerEntry(LocalDate.of(2026, 3, 10), single.id, TestData.NEW_INSTANT))
    )

    @Test
    fun `encode then decode yields the same snapshot`() =
        assertEquals(Outcome.Success(snapshot), codec.decode(codec.encode(snapshot, TestData.EXPORTED_AT)))

    @Test
    fun `encoded file carries metadata and ISO dates`() {
        val text = codec.encode(snapshot, TestData.EXPORTED_AT)
        val document = TestData.json.decodeFromString(ShareDocumentDto.serializer(), text)

        assertEquals(ShareDocumentDto.CURRENT_SCHEMA_VERSION, document.schemaVersion)
        assertEquals(ShareDocumentDto.APP_NAME, document.app)
        assertEquals("2026-05-01T00:00:00Z", document.exportedAt)
        assertEquals("2026-03-11", document.mealDays.last().date)
    }

    @Test
    fun `ingredient order is preserved`() {
        val decoded = decodeSuccess(codec.encode(MenuSnapshot(listOf(starter)), TestData.EXPORTED_AT))

        assertEquals(listOf("Tomato", "Olive oil"), decoded.dishes.single().ingredients.map(Ingredient::name))
    }

    @Test
    fun `files without dinners still import`() {
        val text = """{"exportedAt":"2026-05-01T00:00:00Z","dishes":[],"mealDays":[]}"""

        assertTrue(decodeSuccess(text).dinners.isEmpty())
    }

    @Test
    fun `day of an older file with a dessert imports without it`() {
        val text = """{"exportedAt":"2026-05-01T00:00:00Z","dishes":[],"mealDays":[{"date":"2026-03-11","singleId":"s1","dessertId":"d1","updatedAt":"2026-01-01T00:00:00Z"}]}"""

        assertEquals(DishId("s1"), decodeSuccess(text).lunches.single().singleId)
    }

    @Test
    fun `dish without rating and meal time imports with defaults`() {
        val text = """{"exportedAt":"2026-05-01T00:00:00Z","dishes":[{"id":"d1","name":"Soup","description":"","ingredients":[],"preparation":"","type":"STARTER","heaviness":"VERY_LOW","updatedAt":"2026-01-01T00:00:00Z"}],"mealDays":[]}"""

        val dish = decodeSuccess(text).dishes.single()

        assertEquals(Rating.THREE, dish.rating)
        assertEquals(MealTime.ANY, dish.mealTime)
    }

    @Test
    fun `missing schema version is treated as current and unknown keys are ignored`() =
        assertTrue(codec.decode("""{"exportedAt":"2026-05-01T00:00:00Z","dishes":[],"mealDays":[],"futureField":42}""") is Outcome.Success)

    @Test
    fun `malformed or incomplete files are InvalidImportFile`() = listOf(
        "{ this is not json",
        "",
        "[]",
        """{"schemaVersion":1}""",
        """{"schemaVersion":"one","exportedAt":"x","dishes":[],"mealDays":[]}"""
    ).forEach(::assertInvalid)

    @Test
    fun `newer schema version is UnsupportedImportVersion even with an unparseable body`() {
        assertEquals(Outcome.Failure(DomainError.UnsupportedImportVersion), codec.decode("""{"schemaVersion":2,"exportedAt":"2026-05-01T00:00:00Z","dishes":[],"mealDays":[]}"""))
        assertEquals(Outcome.Failure(DomainError.UnsupportedImportVersion), codec.decode("""{"schemaVersion":2,"somethingNew":true}"""))
    }

    @Test
    fun `unknown enum names are InvalidImportFile`() = listOf(
        dishDto(type = "BRUNCH"),
        dishDto(heaviness = "LIGHT"),
        dishDto(rating = "SIX"),
        dishDto(mealTime = "BREAKFAST"),
        dishDto(ingredients = listOf(IngredientDto("Flour", 1.0, "OUNCE")))
    ).forEach { assertInvalid(textWith(it)) }

    @Test
    fun `dishes breaking the dish rules are InvalidImportFile`() = listOf(
        dishDto(name = "  "),
        dishDto(ingredients = listOf(IngredientDto(" ", 1.0, "GRAM"))),
        dishDto(ingredients = listOf(IngredientDto("Flour", -1.0, "GRAM"))),
        dishDto(ingredients = listOf(IngredientDto("Flour", Double.NaN, "GRAM")))
    ).forEach { assertInvalid(textWith(it)) }

    @Test
    fun `malformed instants and dates are InvalidImportFile`() {
        assertInvalid(textWith(dishDto(updatedAt = "yesterday")))
        assertInvalid(textWith(dishDto(), MealDayDto(date = "2026-13-40", singleId = "d1", updatedAt = "2026-01-01T00:00:00Z")))
    }

    @Test
    fun `repeated dish id keeps the newest version`() {
        val text = textWith(dishDto(name = "Newer", updatedAt = "2026-02-01T00:00:00Z"), dishDto(name = "Older", updatedAt = "2026-01-01T00:00:00Z"))

        assertEquals(listOf("Newer"), decodeSuccess(text).dishes.map { it.name })
    }

    @Test
    fun `repeated lunch date keeps the newest version`() {
        val newer = MealDayDto(date = "2026-03-10", singleId = "new", updatedAt = "2026-02-01T00:00:00Z")
        val older = MealDayDto(date = "2026-03-10", singleId = "old", updatedAt = "2026-01-01T00:00:00Z")

        assertEquals(listOf(DishId("new")), decodeSuccess(textWith(dishDto(), newer, older)).lunches.map { it.singleId })
    }

    private fun assertInvalid(text: String) = assertEquals(text, Outcome.Failure(DomainError.InvalidImportFile), codec.decode(text))

    private fun decodeSuccess(text: String): MenuSnapshot = (codec.decode(text) as Outcome.Success).value

    private fun textWith(vararg items: Any): String = TestData.json.encodeToString(
        ShareDocumentDto.serializer(),
        ShareDocumentDto(
            exportedAt = "2026-05-01T00:00:00Z",
            dishes = items.filterIsInstance<DishDto>(),
            mealDays = items.filterIsInstance<MealDayDto>()
        )
    )

    private fun dishDto(
        name: String = "Dish",
        type: String = "MAIN",
        heaviness: String = "MEDIUM",
        rating: String = "THREE",
        mealTime: String = "ANY",
        ingredients: List<IngredientDto> = emptyList(),
        updatedAt: String = "2026-01-01T00:00:00Z"
    ) = DishDto(
        id = "d1",
        name = name,
        description = "",
        ingredients = ingredients,
        preparation = "",
        type = type,
        heaviness = heaviness,
        rating = rating,
        mealTime = mealTime,
        updatedAt = updatedAt
    )
}
