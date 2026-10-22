package com.gabrieltagama.menuplanner.core.domain.model

import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests DailyMenu.dishes ordering for both variants and the single rule that builds a menu: of
 * takes the slots of one shape and ignores the rest, fromSlots and MenuShape.of accept only the
 * exact slots of one shape, so a single dish never mixes with a starter or a main.
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

    @Test
    fun `of builds the menu of the requested shape ignoring other slots`() {
        val selections = mapOf(MenuSlot.STARTER to starter, MenuSlot.MAIN to main, MenuSlot.SINGLE to single)

        assertEquals(DailyMenu.Courses(starter, main), DailyMenu.of(MenuShape.COURSES, selections))
        assertEquals(DailyMenu.Single(single), DailyMenu.of(MenuShape.SINGLE, selections))
    }

    @Test
    fun `of needs every slot of the shape`() =
        assertNull(DailyMenu.of(MenuShape.COURSES, mapOf(MenuSlot.STARTER to starter)))

    @Test
    fun `fromSlots accepts only the exact slots of one shape`() {
        assertEquals(DailyMenu.Courses(starter, main), DailyMenu.fromSlots(mapOf(MenuSlot.STARTER to starter, MenuSlot.MAIN to main)))
        assertEquals(DailyMenu.Single(single), DailyMenu.fromSlots(mapOf(MenuSlot.SINGLE to single)))
        assertNull(DailyMenu.fromSlots(mapOf(MenuSlot.STARTER to starter, MenuSlot.SINGLE to single)))
        assertNull(DailyMenu.fromSlots(mapOf(MenuSlot.STARTER to starter, MenuSlot.MAIN to main, MenuSlot.SINGLE to single)))
        assertNull(DailyMenu.fromSlots(emptyMap()))
    }

    @Test
    fun `shape of filled slots`() {
        assertEquals(MenuShape.COURSES, MenuShape.of(setOf(MenuSlot.MAIN, MenuSlot.STARTER)))
        assertEquals(MenuShape.SINGLE, MenuShape.of(setOf(MenuSlot.SINGLE)))
        assertNull(MenuShape.of(setOf(MenuSlot.MAIN, MenuSlot.SINGLE)))
        assertNull(MenuShape.of(setOf(MenuSlot.MAIN)))
    }
}
