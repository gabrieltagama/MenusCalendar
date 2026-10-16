package com.gabrieltagama.menuplanner.core.domain.model.share

import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests that planned days of a snapshot resolve with the same rules as planning them by hand: one
 * menu shape, known dishes, each dish in the slot of its type, and dinners only with dinner options.
 */
class MenuSnapshotTest {
    private val date = LocalDate.of(2026, 3, 10)
    private val starter = dish(id = "a", type = DishType.STARTER)
    private val main = dish(id = "b", type = DishType.MAIN)
    private val single = dish(id = "s", type = DishType.SINGLE)
    private val dessert = dish(id = "d", type = DishType.DESSERT)
    private val lunchOnly = dish(id = "l", type = DishType.SINGLE, mealTime = MealTime.LUNCH)
    private val known = listOf(starter, main, single, dessert, lunchOnly).associateBy { it.id }

    @Test
    fun `courses entry with known dishes resolves`() =
        assertEquals(MealDay(date, DailyMenu.Courses(starter, main), TestDishes.OLD_INSTANT), lunch(starterId = "a", mainId = "b").resolve(known))

    @Test
    fun `single entry with a known dish resolves`() =
        assertEquals(MealDay(date, DailyMenu.Single(single), TestDishes.OLD_INSTANT), lunch(singleId = "s").resolve(known))

    @Test
    fun `entry with an unknown dish does not resolve`() = assertNull(lunch(starterId = "a", mainId = "missing").resolve(known))

    @Test
    fun `entry with starter but no main does not resolve`() = assertNull(lunch(starterId = "a").resolve(known))

    @Test
    fun `entry with single and courses at once does not resolve`() = assertNull(lunch(starterId = "a", mainId = "b", singleId = "s").resolve(known))

    @Test
    fun `entry without dishes does not resolve`() = assertNull(lunch().resolve(known))

    @Test
    fun `dessert placed as starter does not resolve`() = assertNull(lunch(starterId = "d", mainId = "b").resolve(known))

    @Test
    fun `main placed as single does not resolve`() = assertNull(lunch(singleId = "b").resolve(known))

    @Test
    fun `dinner with a dinner option resolves`() =
        assertEquals(DinnerDay(date, main, TestDishes.OLD_INSTANT), DinnerEntry(date, DishId("b"), TestDishes.OLD_INSTANT).resolve(known))

    @Test
    fun `dinner with a dessert, a lunch-only or an unknown dish does not resolve`() {
        listOf("d", "l", "missing").forEach { id -> assertNull(id, DinnerEntry(date, DishId(id), TestDishes.OLD_INSTANT).resolve(known)) }
    }

    @Test
    fun `planned days round trip through entries`() {
        val lunch = MealDay(date, DailyMenu.Courses(starter, main), TestDishes.OLD_INSTANT)
        val dinner = DinnerDay(date, single, TestDishes.OLD_INSTANT)

        assertEquals(lunch, LunchEntry.of(lunch).resolve(known))
        assertEquals(dinner, DinnerEntry.of(dinner).resolve(known))
    }

    private fun lunch(starterId: String? = null, mainId: String? = null, singleId: String? = null) = LunchEntry(
        date = date,
        starterId = starterId?.let(::DishId),
        mainId = mainId?.let(::DishId),
        singleId = singleId?.let(::DishId),
        updatedAt = TestDishes.OLD_INSTANT
    )
}
