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
 * Tests that ObserveDishesUseCase returns every dish of the recipe book.
 */
class ObserveDishesUseCaseTest {
    private val starter = dish(id = "s", type = DishType.STARTER)
    private val main = dish(id = "m", type = DishType.MAIN)
    private val dessert = dish(id = "d", type = DishType.DESSERT)

    @Test
    fun `emits all dishes`() = runBlocking {
        assertEquals(setOf(starter, main, dessert), ObserveDishesUseCase(FakeDishRepository(listOf(starter, main, dessert)))().first().toSet())
    }

    @Test
    fun `empty recipe book emits empty list`() = runBlocking {
        assertTrue(ObserveDishesUseCase(FakeDishRepository())().first().isEmpty())
    }
}
