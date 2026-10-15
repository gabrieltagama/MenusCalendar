package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that ObserveDishesUseCase returns every dish or only those of the requested type, sorted
 * by name in Spanish order (case and accents ignored).
 */
class ObserveDishesUseCaseTest {
    private val starter = dish(id = "s", type = DishType.STARTER)
    private val main = dish(id = "m", type = DishType.MAIN)
    private val dessert = dish(id = "d", type = DishType.DESSERT)
    private val useCase = ObserveDishesUseCase(FakeDishRepository(listOf(starter, main, dessert)))

    @Test
    fun `null type emits all dishes`() = runBlocking {
        assertEquals(setOf(starter, main, dessert), useCase().first().toSet())
    }

    @Test
    fun `type emits only dishes of that type`() = runBlocking {
        assertEquals(listOf(dessert), useCase(DishType.DESSERT).first())
    }

    @Test
    fun `type without dishes emits empty list`() = runBlocking {
        assertTrue(useCase(DishType.SINGLE).first().isEmpty())
    }

    @Test
    fun `dishes are sorted by name ignoring case and accents`() = runBlocking {
        val names = listOf("ñoquis", "Arroz", "árbol de verduras", "Natillas", "bacalao")
        val sorted = ObserveDishesUseCase(FakeDishRepository(names.map { dish(id = it, name = it) }))().first().map { it.name }

        assertEquals(listOf("árbol de verduras", "Arroz", "bacalao", "Natillas", "ñoquis"), sorted)
    }
}
