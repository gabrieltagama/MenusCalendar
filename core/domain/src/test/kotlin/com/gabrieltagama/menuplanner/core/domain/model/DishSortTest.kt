package com.gabrieltagama.menuplanner.core.domain.model

import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * DishSort orders and DishUsage: Spanish name order ignoring accents and case, never served first
 * and then the oldest, best rating first, lightest first, ties by name.
 */
class DishSortTest {
    private val today = LocalDate.of(2026, 3, 20)
    private val noquis = dish(id = "a", name = "Ñoquis", rating = Rating.TWO, heaviness = Heaviness.VERY_HIGH)
    private val berenjena = dish(id = "b", name = "berenjena", rating = Rating.FIVE, heaviness = Heaviness.MEDIUM)
    private val arroz = dish(id = "c", name = "Árroz", rating = Rating.FIVE, heaviness = Heaviness.VERY_LOW)
    private val dishes = listOf(noquis, berenjena, arroz)
    private val usage = DishUsage(today, mapOf(noquis.id to LocalDate.of(2026, 3, 18), berenjena.id to LocalDate.of(2026, 2, 1)))

    @Test
    fun `name order ignores accents and case and puts ene after n`() {
        assertEquals(listOf(arroz, berenjena, noquis), DishSort.NAME.sorted(dishes))
    }

    @Test
    fun `last served order puts never served first then oldest`() {
        assertEquals(listOf(arroz, berenjena, noquis), DishSort.LAST_SERVED.sorted(dishes, usage))
    }

    @Test
    fun `rating order puts best first and breaks ties by name`() {
        assertEquals(listOf(arroz, berenjena, noquis), DishSort.RATING.sorted(dishes))
    }

    @Test
    fun `heaviness order puts lightest first`() {
        assertEquals(listOf(arroz, berenjena, noquis), DishSort.HEAVINESS.sorted(dishes))
    }

    @Test
    fun `usage counts days since last served`() {
        assertEquals(2L, usage.daysSinceServed(noquis.id))
        assertNull(usage.daysSinceServed(arroz.id))
    }
}
