package com.gabrieltagama.menuplanner.core.domain.model

import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests DailyMenu.dishes ordering for both variants.
 */
class DailyMenuTest {
    private val starter = dish(type = DishType.STARTER)
    private val main = dish(type = DishType.MAIN)
    private val single = dish(type = DishType.SINGLE)

    @Test
    fun `courses lists starter and main`() =
        assertEquals(listOf(starter, main), DailyMenu.Courses(starter, main).dishes)

    @Test
    fun `single lists only single`() =
        assertEquals(listOf(single), DailyMenu.Single(single).dishes)
}
