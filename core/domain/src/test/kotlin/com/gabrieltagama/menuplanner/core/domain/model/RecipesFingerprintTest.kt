package com.gabrieltagama.menuplanner.core.domain.model

import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Tests that the recipes fingerprint ignores order and changes on add, edit and delete.
 */
class RecipesFingerprintTest {
    private val first = dish(id = "a")
    private val second = dish(id = "b")

    @Test
    fun `order of dishes does not matter`() =
        assertEquals(RecipesFingerprint.of(listOf(first, second)), RecipesFingerprint.of(listOf(second, first)))

    @Test
    fun `adding a dish changes the fingerprint`() =
        assertNotEquals(RecipesFingerprint.of(listOf(first)), RecipesFingerprint.of(listOf(first, second)))

    @Test
    fun `deleting a dish changes the fingerprint`() =
        assertNotEquals(RecipesFingerprint.of(listOf(first, second)), RecipesFingerprint.of(listOf(second)))

    @Test
    fun `editing a dish changes the fingerprint`() =
        assertNotEquals(
            RecipesFingerprint.of(listOf(first)),
            RecipesFingerprint.of(listOf(first.copy(updatedAt = TestDishes.FIXED_INSTANT)))
        )
}
