package com.gabrieltagama.menuplanner.core.domain.model

import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests DailyMenu.dishes ordering, dessert handling, slots and the slot-to-type rule for both variants.
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

    @Test
    fun `slots map every filled place to its dish`() {
        assertEquals(mapOf(MenuSlot.STARTER to starter, MenuSlot.MAIN to main), DailyMenu.Courses(starter, main).slots)
        assertEquals(mapOf(MenuSlot.SINGLE to single, MenuSlot.DESSERT to dessert), DailyMenu.Single(single, dessert).slots)
    }

    @Test
    fun `well placed menu has no misplaced slot`() = assertNull(DailyMenu.Courses(starter, main, dessert).misplacedSlot)

    @Test
    fun `first dish of another type is the misplaced slot`() {
        assertEquals(MenuSlot.MAIN, DailyMenu.Courses(starter, single, dessert).misplacedSlot)
        assertEquals(MenuSlot.DESSERT, DailyMenu.Single(single, main).misplacedSlot)
    }

    @Test
    fun `every slot accepts only its own dish type`() =
        MenuSlot.entries.forEach { slot -> DishType.entries.forEach { type -> assertEquals(type == slot.dishType, slot.accepts(type)) } }
}
