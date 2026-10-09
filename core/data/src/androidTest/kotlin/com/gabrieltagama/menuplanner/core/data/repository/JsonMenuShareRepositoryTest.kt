package com.gabrieltagama.menuplanner.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.data.database.AndroidTestData
import com.gabrieltagama.menuplanner.core.data.database.MenuPlannerDatabase
import com.gabrieltagama.menuplanner.core.data.share.ShareDocumentCodec
import com.gabrieltagama.menuplanner.core.data.share.ShareMerger
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests of JsonMenuShareRepository on an in-memory database: export, wipe and import
 * restores the same data; importing the same file again skips everything; invalid files leave the
 * database untouched.
 */
@RunWith(AndroidJUnit4::class)
class JsonMenuShareRepositoryTest {
    private lateinit var database: MenuPlannerDatabase
    private lateinit var dishRepository: RoomDishRepository
    private lateinit var mealPlanRepository: RoomMealPlanRepository
    private lateinit var repository: JsonMenuShareRepository

    private val clock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val starter = AndroidTestData.dish(
        "st",
        name = "Gazpacho",
        type = DishType.STARTER,
        ingredients = listOf(Ingredient("Tomato", 500.0, MeasureUnit.GRAM), Ingredient("Oil", 2.0, MeasureUnit.TABLESPOON))
    )
    private val main = AndroidTestData.dish("mn", name = "Paella", type = DishType.MAIN, updatedAt = AndroidTestData.NEW_INSTANT)
    private val single = AndroidTestData.dish("sg", name = "Lasagna", type = DishType.SINGLE)
    private val dessert = AndroidTestData.dish("ds", name = "Flan", type = DishType.DESSERT)
    private val coursesDay = AndroidTestData.coursesDay(LocalDate.of(2026, 3, 10), starter, main, dessert)
    private val singleDay = AndroidTestData.singleDay(LocalDate.of(2026, 3, 11), single, updatedAt = AndroidTestData.NEW_INSTANT)

    @Before
    fun setUp() {
        database = AndroidTestData.inMemoryDatabase()
        createRepositories()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun exportClearImportRoundTripRestoresEverything() = runTest {
        seed()
        val exported = repository.exportAll()
        database.close()
        database = AndroidTestData.inMemoryDatabase()
        createRepositories()

        val result = repository.importAndMerge(exported)

        assertEquals(Outcome.Success(ImportSummary(4, 0, 0, 2, 0, 0)), result)
        assertEquals(listOf(dessert, starter, single, main), dishRepository.observeDishes().first())
        assertEquals(coursesDay, mealPlanRepository.getDay(coursesDay.date))
        assertEquals(singleDay, mealPlanRepository.getDay(singleDay.date))
    }

    @Test
    fun secondImportOfTheSameFileSkipsEverything() = runTest {
        seed()
        val exported = repository.exportAll()

        val result = repository.importAndMerge(exported)

        assertEquals(Outcome.Success(ImportSummary(0, 0, 4, 0, 0, 2)), result)
    }

    @Test
    fun importIntoEmptyDatabaseThenAgainSkipsEverything() = runTest {
        seed()
        val exported = repository.exportAll()
        database.close()
        database = AndroidTestData.inMemoryDatabase()
        createRepositories()
        repository.importAndMerge(exported)

        val second = repository.importAndMerge(exported)

        assertEquals(Outcome.Success(ImportSummary(0, 0, 4, 0, 0, 2)), second)
    }

    @Test
    fun exportIsStableAndCarriesExportTimestamp() = runTest {
        seed()

        val first = repository.exportAll()

        assertEquals(first, repository.exportAll())
        assertTrue(first.contains("2026-05-01T00:00:00Z"))
    }

    @Test
    fun invalidFileDoesNotTouchTheDatabase() = runTest {
        seed()

        assertEquals(Outcome.Failure(DomainError.InvalidImportFile), repository.importAndMerge("not json"))
        assertEquals(4, dishRepository.observeDishes().first().size)
    }

    @Test
    fun newerSchemaVersionIsRejected() = runTest {
        val text = """{"schemaVersion":2,"exportedAt":"2026-05-01T00:00:00Z","dishes":[],"mealDays":[]}"""

        assertEquals(Outcome.Failure(DomainError.UnsupportedImportVersion), repository.importAndMerge(text))
    }

    @Test
    fun dayReferencingUnknownDishIsSkippedOnImport() = runTest {
        val text = """{"schemaVersion":1,"exportedAt":"2026-05-01T00:00:00Z","dishes":[],"mealDays":[{"date":"2026-03-12","singleId":"ghost","updatedAt":"2026-01-01T00:00:00Z"}]}"""

        assertEquals(Outcome.Success(ImportSummary(0, 0, 0, 0, 0, 1)), repository.importAndMerge(text))
        assertNull(mealPlanRepository.getDay(LocalDate.of(2026, 3, 12)))
    }

    private suspend fun seed() {
        listOf(starter, main, single, dessert).forEach { dishRepository.upsert(it) }
        mealPlanRepository.save(coursesDay)
        mealPlanRepository.save(singleDay)
    }

    private fun createRepositories() {
        val dishDao = database.dishDao()
        val mealDayDao = database.mealDayDao()
        dishRepository = RoomDishRepository(dishDao)
        mealPlanRepository = RoomMealPlanRepository(mealDayDao, dishDao)
        repository = JsonMenuShareRepository(database, ShareDocumentCodec(json), ShareMerger(dishDao, mealDayDao), clock)
    }
}
