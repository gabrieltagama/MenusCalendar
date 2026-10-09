package com.gabrieltagama.menuplanner.core.domain.model

import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests DailyMenu.dishes ordering and dessert handling for both variants.
 */
class DailyMenuTest {
    private val starter = dish(type = DishType.STARTER)
    private val main = dish(type = DishType.MAIN)
    private val single = dish(type = DishType.SINGLE)
    private val dessert = dish(type = DishType.DESSERT)

    @Test
    fun `courses with dessert lists starter main dessert`() =
        assertEquals(listOf(starter, main, dessert), DailyMenu.Courses(starter, main, dessert).dishes)

    @Test
    fun `courses without dessert lists starter and main`() =
        assertEquals(listOf(starter, main), DailyMenu.Courses(starter, main).dishes)

    @Test
    fun `single with dessert lists single and dessert`() =
        assertEquals(listOf(single, dessert), DailyMenu.Single(single, dessert).dishes)

    @Test
    fun `single without dessert lists only single`() =
        assertEquals(listOf(single), DailyMenu.Single(single).dishes)
}
