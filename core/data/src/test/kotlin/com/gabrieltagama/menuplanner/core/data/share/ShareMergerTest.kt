package com.gabrieltagama.menuplanner.core.data.share

import com.gabrieltagama.menuplanner.core.data.database.entity.DinnerDayEntity
import com.gabrieltagama.menuplanner.core.data.fake.FakeDinnerDayDao
import com.gabrieltagama.menuplanner.core.data.fake.FakeDishDao
import com.gabrieltagama.menuplanner.core.data.fake.FakeMealDayDao
import com.gabrieltagama.menuplanner.core.data.fake.TestData
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.data.mapper.toIngredientEntities
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests ShareMerger against in-memory DAOs: add/update/skip counts for dishes and days, and that
 * a day referencing a dish that is neither local nor in the file is skipped; dinners are merged
 * with the same rules, counted with the days, and dinners whose dish is not a dinner option (a
 * dessert or a lunch-only dish) are skipped.
 */
class ShareMergerTest {
    private val mealDayDao = FakeMealDayDao()
    private val dishDao = FakeDishDao(mealDayDao)
    private val dinnerDayDao = FakeDinnerDayDao()
    private val merger = ShareMerger(dishDao, mealDayDao, dinnerDayDao)
    private val date = LocalDate.of(2026, 3, 10)

    @Test
    fun `new dishes and days are added`() = runTest {
        val single = TestData.dish("s", type = DishType.SINGLE)

        val summary = merger.merge(ImportContent(listOf(single), listOf(TestData.singleDay(date, "s"))))

        assertEquals(ImportSummary(1, 0, 0, 1, 0, 0), summary)
        assertTrue("s" in dishDao.currentDishes)
        assertTrue(date.toEpochDay() in mealDayDao.currentDays)
    }

    @Test
    fun `newer dish replaces local and older or equal dishes are skipped`() = runTest {
        insert(TestData.dish("newer", name = "Local newer"))
        insert(TestData.dish("older", name = "Local older", updatedAt = TestData.NEW_INSTANT))
        insert(TestData.dish("equal", name = "Local equal"))
        val incoming = listOf(
            TestData.dish("newer", name = "Remote newer", updatedAt = TestData.NEW_INSTANT),
            TestData.dish("older", name = "Remote older", updatedAt = TestData.OLD_INSTANT),
            TestData.dish("equal", name = "Remote equal", updatedAt = TestData.OLD_INSTANT)
        )

        val summary = merger.merge(ImportContent(incoming, emptyList()))

        assertEquals(ImportSummary(0, 1, 2, 0, 0, 0), summary)
        assertEquals("Remote newer", dishDao.currentDishes.getValue("newer").name)
        assertEquals("Local older", dishDao.currentDishes.getValue("older").name)
        assertEquals("Local equal", dishDao.currentDishes.getValue("equal").name)
    }

    @Test
    fun `day referencing unknown dish is skipped`() = runTest {
        val summary = merger.merge(ImportContent(emptyList(), listOf(TestData.singleDay(date, "ghost"))))

        assertEquals(ImportSummary(0, 0, 0, 0, 0, 1), summary)
        assertNull(mealDayDao.getByDate(date.toEpochDay()))
    }

    @Test
    fun `day can reference a dish already stored locally`() = runTest {
        insert(TestData.dish("s", type = DishType.SINGLE))

        val summary = merger.merge(ImportContent(emptyList(), listOf(TestData.singleDay(date, "s"))))

        assertEquals(1, summary.daysAdded)
    }

    @Test
    fun `newer day updates and older day is skipped`() = runTest {
        insert(TestData.dish("a", type = DishType.SINGLE))
        insert(TestData.dish("b", type = DishType.SINGLE))
        val otherDate = date.plusDays(1)
        mealDayDao.upsert(TestData.singleDay(date, "a", updatedAt = TestData.OLD_INSTANT))
        mealDayDao.upsert(TestData.singleDay(otherDate, "a", updatedAt = TestData.NEW_INSTANT))
        val incoming = listOf(
            TestData.singleDay(date, "b", updatedAt = TestData.NEW_INSTANT),
            TestData.singleDay(otherDate, "b", updatedAt = TestData.OLD_INSTANT)
        )

        val summary = merger.merge(ImportContent(emptyList(), incoming))

        assertEquals(ImportSummary(0, 0, 0, 0, 1, 1), summary)
        assertEquals("b", mealDayDao.currentDays.getValue(date.toEpochDay()).singleId)
        assertEquals("a", mealDayDao.currentDays.getValue(otherDate.toEpochDay()).singleId)
    }

    @Test
    fun `merging the same content twice skips everything the second time`() = runTest {
        val content = ImportContent(
            listOf(TestData.dish("x", type = DishType.STARTER), TestData.dish("y", type = DishType.MAIN)),
            listOf(TestData.coursesDay(date, "x", "y"))
        )
        merger.merge(content)

        val summary = merger.merge(content)

        assertEquals(ImportSummary(0, 0, 2, 0, 0, 1), summary)
    }

    @Test
    fun `day with a dish in the wrong slot is skipped`() = runTest {
        val content = ImportContent(
            listOf(TestData.dish("d", type = DishType.DESSERT), TestData.dish("m", type = DishType.MAIN)),
            listOf(TestData.coursesDay(date, "d", "m"))
        )

        val summary = merger.merge(content)

        assertEquals(ImportSummary(2, 0, 0, 0, 0, 1), summary)
        assertNull(mealDayDao.getByDate(date.toEpochDay()))
    }

    private suspend fun insert(dish: Dish) = dishDao.upsertWithIngredients(dish.toEntity(), dish.toIngredientEntities())

    @Test
    fun `dinners are added counted with days and newer local dinners are kept`() = runTest {
        insert(TestData.dish("m", type = DishType.MAIN))
        dinnerDayDao.upsert(DinnerDayEntity(date.plusDays(1).toEpochDay(), "m", updatedAt = TestData.NEW_INSTANT.toEpochMilli()))
        val incoming = listOf(
            DinnerDayEntity(date.toEpochDay(), "m", updatedAt = TestData.OLD_INSTANT.toEpochMilli()),
            DinnerDayEntity(date.plusDays(1).toEpochDay(), "m", updatedAt = TestData.OLD_INSTANT.toEpochMilli())
        )

        val summary = merger.merge(ImportContent(emptyList(), emptyList(), incoming))

        assertEquals(ImportSummary(0, 0, 0, 1, 0, 1), summary)
        assertEquals("m", dinnerDayDao.currentDays.getValue(date.toEpochDay()).dishId)
        assertEquals(TestData.NEW_INSTANT.toEpochMilli(), dinnerDayDao.currentDays.getValue(date.plusDays(1).toEpochDay()).updatedAt)
    }

    @Test
    fun `dinners with an unknown dish, a dessert or a lunch-only dish are skipped`() = runTest {
        insert(TestData.dish("d", type = DishType.DESSERT))
        insert(TestData.dish("l", type = DishType.SINGLE, mealTime = MealTime.LUNCH))
        val incoming = listOf(
            DinnerDayEntity(date.toEpochDay(), "d", updatedAt = 1),
            DinnerDayEntity(date.plusDays(1).toEpochDay(), "ghost", updatedAt = 1),
            DinnerDayEntity(date.plusDays(2).toEpochDay(), "l", updatedAt = 1)
        )

        val summary = merger.merge(ImportContent(emptyList(), emptyList(), incoming))

        assertEquals(3, summary.daysSkipped)
        assertTrue(dinnerDayDao.currentDays.isEmpty())
    }
}
