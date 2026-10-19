package com.gabrieltagama.menuplanner.core.domain.model.share

import com.gabrieltagama.menuplanner.core.domain.model.DishType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Tests that the natural key of a dish ignores accents, case, punctuation and spacing, and that it
 * distinguishes dishes of different names or types.
 */
class DishKeyTest {

    @Test
    fun `accents case punctuation and spaces are ignored`() =
        assertEquals(DishKey.of("Lentejas con chorizo", DishType.SINGLE), DishKey.of("  LENTÉJAS,  con   chorizo. ", DishType.SINGLE))

    @Test
    fun `different names have different keys`() =
        assertNotEquals(DishKey.of("Lentejas", DishType.SINGLE), DishKey.of("Garbanzos", DishType.SINGLE))

    @Test
    fun `same name with another type has a different key`() =
        assertNotEquals(DishKey.of("Ensalada", DishType.STARTER), DishKey.of("Ensalada", DishType.SINGLE))
}
