package com.gabrieltagama.menuplanner.core.domain.usecase.share

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuShareRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeTransactionRunner
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.ImportContent
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.ImportedDinner
import com.gabrieltagama.menuplanner.core.domain.model.ImportedLunch
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.StandardizeRecipeBookUseCase
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests ExportMenusUseCase delegation and ImportMenusUseCase: decode failures leave the data
 * untouched, last-writer-wins for dishes, lunches and dinners, days that break a calendar rule are
 * skipped, everything runs in one transaction and the recipe book ends in standard units.
 */
class ShareUseCasesTest {
    private val date = LocalDate.of(2026, 3, 10)
    private val newer = TestDishes.FIXED_INSTANT
    private val older = TestDishes.OLD_INSTANT
    private val share = FakeMenuShareRepository()
    private val dishes = FakeDishRepository()
    private val lunches = FakeMealPlanRepository()
    private val dinners = FakeDinnerPlanRepository()
    private val transactions = FakeTransactionRunner()
    private val importMenus = ImportMenusUseCase(share, dishes, lunches, dinners, transactions, StandardizeRecipeBookUseCase(dishes))

    private fun import(content: ImportContent): ImportSummary = runBlocking {
        share.decodeResult = Outcome.Success(content)
        (importMenus("payload") as Outcome.Success).value
    }

    private fun single(dishId: String, dessertId: String? = null, on: LocalDate = date, at: Instant = older) =
        ImportedLunch(on, listOfNotNull(MenuSlot.SINGLE to dishId, dessertId?.let { MenuSlot.DESSERT to it }).toMap(), at)

    private fun courses(starterId: String, mainId: String) = ImportedLunch(date, mapOf(MenuSlot.STARTER to starterId, MenuSlot.MAIN to mainId), older)

    private fun store(vararg stored: Dish) = runBlocking { stored.forEach { dishes.upsert(it) } }

    @Test
    fun `export returns repository json`() = runBlocking {
        val repository = FakeMenuShareRepository(exportJson = """{"dishes":[]}""")

        assertEquals("""{"dishes":[]}""", ExportMenusUseCase(repository)())
        assertEquals(1, repository.exportCalls)
    }

    @Test
    fun `decode failure is returned and nothing is written`() = runBlocking {
        val failure = Outcome.Failure(DomainError.UnsupportedImportVersion)
        share.decodeResult = failure

        assertEquals(failure, importMenus("bad"))
        assertEquals(listOf("bad"), share.decodedJson)
        assertEquals(0, transactions.transactions)
        assertTrue(dishes.upserted.isEmpty())
    }

    @Test
    fun `new dishes and days are added in one transaction`() {
        val summary = import(ImportContent(listOf(dish("s", type = DishType.SINGLE)), listOf(single("s"))))

        assertEquals(ImportSummary(1, 0, 0, 1, 0, 0), summary)
        assertTrue("s" in dishes.current)
        assertTrue(date in lunches.current)
        assertEquals(1, transactions.transactions)
    }

    @Test
    fun `newer dish replaces local and older or equal dishes are skipped`() {
        store(dish("newer", name = "Local newer"), dish("older", name = "Local older", updatedAt = newer), dish("equal", name = "Local equal"))
        val incoming = listOf(
            dish("newer", name = "Remote newer", updatedAt = newer),
            dish("older", name = "Remote older"),
            dish("equal", name = "Remote equal")
        )

        assertEquals(ImportSummary(0, 1, 2, 0, 0, 0), import(ImportContent(incoming)))
        assertEquals("Remote newer", dishes.current.getValue("newer").name)
        assertEquals("Local older", dishes.current.getValue("older").name)
        assertEquals("Local equal", dishes.current.getValue("equal").name)
    }

    @Test
    fun `sub millisecond differences do not count as newer`() {
        store(dish("a", name = "Local", updatedAt = Instant.parse("2026-01-01T00:00:00.123Z")))

        import(ImportContent(listOf(dish("a", name = "Remote", updatedAt = Instant.parse("2026-01-01T00:00:00.123456Z")))))

        assertEquals("Local", dishes.current.getValue("a").name)
    }

    @Test
    fun `day referencing an unknown dish or dessert is skipped`() {
        store(dish("s", type = DishType.SINGLE))

        val summary = import(ImportContent(emptyList(), listOf(single("ghost"), single("s", dessertId = "ghost", on = date.plusDays(1)))))

        assertEquals(ImportSummary(0, 0, 0, 0, 0, 2), summary)
        assertTrue(lunches.current.isEmpty())
    }

    @Test
    fun `day can reference a dish already stored locally`() {
        store(dish("s", type = DishType.SINGLE))

        assertEquals(1, import(ImportContent(emptyList(), listOf(single("s")))).daysAdded)
    }

    @Test
    fun `newer day updates and older day is skipped`() = runBlocking {
        val a = dish("a", type = DishType.SINGLE)
        val b = dish("b", type = DishType.SINGLE)
        store(a, b)
        lunches.save(MealDay(date, DailyMenu.Single(a), older))
        lunches.save(MealDay(date.plusDays(1), DailyMenu.Single(a), newer))

        val summary = import(ImportContent(emptyList(), listOf(single("b", at = newer), single("b", on = date.plusDays(1)))))

        assertEquals(ImportSummary(0, 0, 0, 0, 1, 1), summary)
        assertEquals(b, (lunches.current.getValue(date).menu as DailyMenu.Single).single)
        assertEquals(a, (lunches.current.getValue(date.plusDays(1)).menu as DailyMenu.Single).single)
    }

    @Test
    fun `importing the same content twice skips everything the second time`() {
        val content = ImportContent(listOf(dish("x", type = DishType.STARTER), dish("y", type = DishType.MAIN)), listOf(courses("x", "y")))
        import(content)

        assertEquals(ImportSummary(0, 0, 2, 0, 0, 1), import(content))
    }

    @Test
    fun `day with a dish in the wrong slot or an invalid shape is skipped`() {
        val incomingDishes = listOf(dish("d", type = DishType.DESSERT), dish("m", type = DishType.MAIN), dish("s", type = DishType.SINGLE), dish("a", type = DishType.STARTER))
        val days = listOf(
            courses("d", "m"),
            ImportedLunch(date.plusDays(1), mapOf(MenuSlot.STARTER to "a"), older),
            ImportedLunch(date.plusDays(2), mapOf(MenuSlot.DESSERT to "d"), older),
            ImportedLunch(date.plusDays(3), mapOf(MenuSlot.STARTER to "a", MenuSlot.MAIN to "m", MenuSlot.SINGLE to "s"), older)
        )

        assertEquals(ImportSummary(4, 0, 0, 0, 0, 4), import(ImportContent(incomingDishes, days)))
        assertTrue(lunches.current.isEmpty())
    }

    @Test
    fun `dinners are added counted with days and newer local dinners are kept`() = runBlocking {
        val main = dish("m", type = DishType.MAIN)
        store(main)
        dinners.save(DinnerDay(date.plusDays(1), main, newer))
        val incoming = listOf(ImportedDinner(date, "m", older), ImportedDinner(date.plusDays(1), "m", older))

        assertEquals(ImportSummary(0, 0, 0, 1, 0, 1), import(ImportContent(emptyList(), emptyList(), incoming)))
        assertEquals(main, dinners.current.getValue(date).dish)
        assertEquals(newer, dinners.current.getValue(date.plusDays(1)).updatedAt)
    }

    @Test
    fun `dinners with an unknown dish, a dessert or a lunch only dish are skipped`() {
        store(dish("d", type = DishType.DESSERT), dish("l", type = DishType.MAIN, mealTime = MealTime.LUNCH))
        val incoming = listOf(ImportedDinner(date, "d", older), ImportedDinner(date.plusDays(1), "ghost", older), ImportedDinner(date.plusDays(2), "l", older))

        assertEquals(3, import(ImportContent(emptyList(), emptyList(), incoming)).daysSkipped)
        assertTrue(dinners.current.isEmpty())
    }

    @Test
    fun `import converts the recipe book to standard units`() {
        store(dish(id = "a", ingredients = listOf(Ingredient("Leche", 0.5, MeasureUnit.LITER))))

        import(ImportContent(emptyList()))

        assertEquals(listOf(Ingredient("Leche", 500.0, MeasureUnit.MILLILITER)), dishes.current.getValue("a").ingredients)
    }

    @Test
    fun `merge policy adds unknown, updates newer and skips older or equal`() {
        assertEquals(MergeDecision.ADD, MergePolicy.decide(local = null, incoming = older))
        assertEquals(MergeDecision.UPDATE, MergePolicy.decide(local = older, incoming = newer))
        assertEquals(MergeDecision.SKIP, MergePolicy.decide(local = newer, incoming = older))
        assertEquals(MergeDecision.SKIP, MergePolicy.decide(local = older, incoming = older))
    }

    @Test
    fun `merge count aggregates decisions and invalid items`() {
        val decisions = listOf(MergeDecision.ADD, MergeDecision.SKIP, MergeDecision.ADD, MergeDecision.UPDATE)

        assertEquals(MergeCount(added = 2, updated = 1, skipped = 3), MergeCount.of(decisions, invalid = 2))
    }

    @Test
    fun `resolved lunch keeps its dishes, date and updatedAt`() {
        val starter = dish("a", type = DishType.STARTER)
        val main = dish("m", type = DishType.MAIN)

        val day = courses("a", "m").resolve(mapOf("a" to starter, "m" to main))

        assertEquals(MealDay(date, DailyMenu.Courses(starter, main), older), day)
        assertNull(courses("a", "m").resolve(mapOf("a" to starter)))
    }
}
