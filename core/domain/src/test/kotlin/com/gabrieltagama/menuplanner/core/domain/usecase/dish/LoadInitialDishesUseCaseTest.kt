package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeInitialDishesSource
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuShareRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeTransactionRunner
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.ImportContent
import com.gabrieltagama.menuplanner.core.domain.usecase.share.ImportMenusUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that the starter recipe book is imported through ImportMenusUseCase only on the first
 * launch and only into an empty recipe book, and that it is never offered again.
 */
class LoadInitialDishesUseCaseTest {
    private val share = FakeMenuShareRepository(decodeResult = Outcome.Success(ImportContent(listOf(dish(id = "starter-dish")))))

    private fun useCase(dishes: FakeDishRepository, source: FakeInitialDishesSource) = LoadInitialDishesUseCase(
        source,
        dishes,
        ImportMenusUseCase(share, dishes, FakeMealPlanRepository(), FakeDinnerPlanRepository(), FakeTransactionRunner(), StandardizeRecipeBookUseCase(dishes))
    )

    @Test
    fun `first launch with an empty recipe book imports the starter dishes once`() = runBlocking {
        val dishes = FakeDishRepository()
        val source = FakeInitialDishesSource()
        val load = useCase(dishes, source)

        load()
        load()

        assertTrue("starter-dish" in dishes.current)
        assertEquals(listOf("starter"), share.decodedJson)
        assertTrue(source.loaded)
    }

    @Test
    fun `users who already have dishes never receive the starter book`() = runBlocking {
        val dishes = FakeDishRepository(listOf(dish(id = "mine")))
        val source = FakeInitialDishesSource()

        useCase(dishes, source)()

        assertEquals(0, source.reads)
        assertEquals(setOf("mine"), dishes.current.keys)
        assertTrue(source.loaded)
    }

    @Test
    fun `nothing happens when it was already loaded`() = runBlocking {
        val source = FakeInitialDishesSource(loaded = true)

        useCase(FakeDishRepository(), source)()

        assertEquals(0, source.reads)
    }
}
