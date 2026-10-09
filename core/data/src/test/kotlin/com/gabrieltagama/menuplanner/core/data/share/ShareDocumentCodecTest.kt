package com.gabrieltagama.menuplanner.core.data.share

import com.gabrieltagama.menuplanner.core.data.fake.TestData
import com.gabrieltagama.menuplanner.core.data.share.dto.DishDto
import com.gabrieltagama.menuplanner.core.data.share.dto.IngredientDto
import com.gabrieltagama.menuplanner.core.data.share.dto.MealDayDto
import com.gabrieltagama.menuplanner.core.data.share.dto.ShareDocumentDto
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests ShareDocumentCodec together with ShareDocumentMapper: lossless round trip from the model
 * through JSON and back, and the error mapping of malformed, newer or inconsistent files.
 */
class ShareDocumentCodecTest {
    private val codec = ShareDocumentCodec(TestData.json)

    private val starter = TestData.dish(
        id = "starter-1",
        name = "Gazpacho",
        type = DishType.STARTER,
        heaviness = Heaviness.VERY_LOW,
        ingredients = listOf(
            Ingredient("Tomato", 500.0, MeasureUnit.GRAM),
            Ingredient("Olive oil", 2.5, MeasureUnit.TABLESPOON),
            Ingredient("Salt", 0.0, MeasureUnit.TO_TASTE)
        )
    )
    private val main = TestData.dish(id = "main-1", name = "Paella", type = DishType.MAIN, heaviness = Heaviness.VERY_HIGH, updatedAt = TestData.NEW_INSTANT)
    private val dessert = TestData.dish(id = "dessert-1", name = "Flan", type = DishType.DESSERT)
    private val single = TestData.dish(id = "single-1", name = "Lasagna", type = DishType.SINGLE)

    private val coursesDay = TestData.coursesDay(LocalDate.of(2026, 3, 10), starter.id, main.id, dessert.id, TestData.NEW_INSTANT)
    private val singleDay = TestData.singleDay(LocalDate.of(2026, 3, 11), single.id)

    @Test
    fun `export then import yields the same dishes and days`() {
        val dishes = listOf(starter, main, dessert, single)
        val document = ShareDocumentMapper.toDocument(dishes, listOf(coursesDay, singleDay), TestData.EXPORTED_AT)

        val decoded = codec.decode(codec.encode(document))

        assertEquals(Outcome.Success(document), decoded)
        val content = (ShareDocumentMapper.toImportContent((decoded as Outcome.Success).value) as Outcome.Success).value
        assertEquals(dishes, content.dishes)
        assertEquals(listOf(coursesDay, singleDay), content.mealDays)
    }

    @Test
    fun `exported document carries metadata and ISO dates`() {
        val document = ShareDocumentMapper.toDocument(listOf(single), listOf(singleDay), TestData.EXPORTED_AT)

        assertEquals(ShareDocumentDto.CURRENT_SCHEMA_VERSION, document.schemaVersion)
        assertEquals(ShareDocumentDto.APP_NAME, document.app)
        assertEquals("2026-05-01T00:00:00Z", document.exportedAt)
        assertEquals("2026-03-11", document.mealDays.single().date)
        assertEquals("SINGLE", document.dishes.single().type)
    }

    @Test
    fun `ingredient order is preserved`() {
        val document = ShareDocumentMapper.toDocument(listOf(starter), emptyList(), TestData.EXPORTED_AT)

        val content = (ShareDocumentMapper.toImportContent(document) as Outcome.Success).value

        assertEquals(listOf("Tomato", "Olive oil", "Salt"), content.dishes.single().ingredients.map(Ingredient::name))
    }

    @Test
    fun `malformed JSON is InvalidImportFile`() =
        assertEquals(Outcome.Failure(DomainError.InvalidImportFile), codec.decode("{ this is not json"))

    @Test
    fun `empty text is InvalidImportFile`() =
        assertEquals(Outcome.Failure(DomainError.InvalidImportFile), codec.decode(""))

    @Test
    fun `JSON array root is InvalidImportFile`() =
        assertEquals(Outcome.Failure(DomainError.InvalidImportFile), codec.decode("[]"))

    @Test
    fun `missing required fields is InvalidImportFile`() =
        assertEquals(Outcome.Failure(DomainError.InvalidImportFile), codec.decode("""{"schemaVersion":1}"""))

    @Test
    fun `non numeric schema version is InvalidImportFile`() =
        assertEquals(Outcome.Failure(DomainError.InvalidImportFile), codec.decode("""{"schemaVersion":"one","exportedAt":"x","dishes":[],"mealDays":[]}"""))

    @Test
    fun `schema version 2 is UnsupportedImportVersion`() {
        val text = """{"schemaVersion":2,"exportedAt":"2026-05-01T00:00:00Z","dishes":[],"mealDays":[]}"""

        assertEquals(Outcome.Failure(DomainError.UnsupportedImportVersion), codec.decode(text))
    }

    @Test
    fun `newer schema version wins over an otherwise unparseable body`() =
        assertEquals(Outcome.Failure(DomainError.UnsupportedImportVersion), codec.decode("""{"schemaVersion":2,"somethingNew":true}"""))

    @Test
    fun `missing schema version is treated as current and unknown keys are ignored`() {
        val text = """{"exportedAt":"2026-05-01T00:00:00Z","dishes":[],"mealDays":[],"futureField":42}"""

        val decoded = codec.decode(text)

        assertTrue(decoded is Outcome.Success)
        assertEquals(ShareDocumentDto.CURRENT_SCHEMA_VERSION, (decoded as Outcome.Success).value.schemaVersion)
    }

    @Test
    fun `unknown dish type is InvalidImportFile`() =
        assertEquals(Outcome.Failure(DomainError.InvalidImportFile), ShareDocumentMapper.toImportContent(documentWith(dishDto(type = "BRUNCH"))))

    @Test
    fun `unknown heaviness is InvalidImportFile`() =
        assertEquals(Outcome.Failure(DomainError.InvalidImportFile), ShareDocumentMapper.toImportContent(documentWith(dishDto(heaviness = "LIGHT"))))

    @Test
    fun `unknown measure unit is InvalidImportFile`() {
        val dto = dishDto(ingredients = listOf(IngredientDto("Flour", 1.0, "OUNCE")))

        assertEquals(Outcome.Failure(DomainError.InvalidImportFile), ShareDocumentMapper.toImportContent(documentWith(dto)))
    }

    @Test
    fun `unknown enum inside full JSON is InvalidImportFile end to end`() {
        val text = codec.encode(documentWith(dishDto(type = "BRUNCH")))

        val decoded = (codec.decode(text) as Outcome.Success).value

        assertEquals(Outcome.Failure(DomainError.InvalidImportFile), ShareDocumentMapper.toImportContent(decoded))
    }

    @Test
    fun `malformed dish instant is InvalidImportFile`() =
        assertEquals(Outcome.Failure(DomainError.InvalidImportFile), ShareDocumentMapper.toImportContent(documentWith(dishDto(updatedAt = "yesterday"))))

    @Test
    fun `malformed meal day date is InvalidImportFile`() {
        val document = documentWith(dishDto(), MealDayDto(date = "2026-13-40", singleId = "d1", updatedAt = "2026-01-01T00:00:00Z"))

        assertEquals(Outcome.Failure(DomainError.InvalidImportFile), ShareDocumentMapper.toImportContent(document))
    }

    @Test
    fun `blank dish name is InvalidImportFile`() =
        assertEquals(Outcome.Failure(DomainError.InvalidImportFile), ShareDocumentMapper.toImportContent(documentWith(dishDto(name = "  "))))

    @Test
    fun `blank ingredient name is InvalidImportFile`() {
        val dto = dishDto(ingredients = listOf(IngredientDto(" ", 1.0, "GRAM")))

        assertEquals(Outcome.Failure(DomainError.InvalidImportFile), ShareDocumentMapper.toImportContent(documentWith(dto)))
    }

    @Test
    fun `negative ingredient quantity is InvalidImportFile`() {
        val dto = dishDto(ingredients = listOf(IngredientDto("Flour", -1.0, "GRAM")))

        assertEquals(Outcome.Failure(DomainError.InvalidImportFile), ShareDocumentMapper.toImportContent(documentWith(dto)))
    }

    @Test
    fun `non finite ingredient quantity is InvalidImportFile`() {
        val dto = dishDto(ingredients = listOf(IngredientDto("Flour", Double.NaN, "GRAM")))

        assertEquals(Outcome.Failure(DomainError.InvalidImportFile), ShareDocumentMapper.toImportContent(documentWith(dto)))
    }

    @Test
    fun `repeated dish id keeps the newest version`() {
        val older = dishDto(name = "Older", updatedAt = "2026-01-01T00:00:00Z")
        val newer = dishDto(name = "Newer", updatedAt = "2026-02-01T00:00:00Z")
        val document = ShareDocumentDto(exportedAt = "2026-05-01T00:00:00Z", dishes = listOf(newer, older), mealDays = emptyList())

        val content = (ShareDocumentMapper.toImportContent(document) as Outcome.Success).value

        assertEquals(listOf("Newer"), content.dishes.map { it.name })
    }

    @Test
    fun `repeated meal day date keeps the newest version`() {
        val newer = MealDayDto(date = "2026-03-10", singleId = "new", updatedAt = "2026-02-01T00:00:00Z")
        val older = MealDayDto(date = "2026-03-10", singleId = "old", updatedAt = "2026-01-01T00:00:00Z")

        val content = (ShareDocumentMapper.toImportContent(documentWith(dishDto(), newer, older)) as Outcome.Success).value

        assertEquals(listOf("new"), content.mealDays.map { it.singleId })
    }

    private fun dishDto(
        name: String = "Dish",
        type: String = "MAIN",
        heaviness: String = "MEDIUM",
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
        updatedAt = updatedAt
    )

    private fun documentWith(dish: DishDto, vararg days: MealDayDto) =
        ShareDocumentDto(exportedAt = "2026-05-01T00:00:00Z", dishes = listOf(dish), mealDays = days.toList())
}
