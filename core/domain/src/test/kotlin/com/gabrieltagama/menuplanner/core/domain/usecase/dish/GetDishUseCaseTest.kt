package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests GetDishUseCase success and DishNotFound failure.
 */
class GetDishUseCaseTest {
    private val stored = dish(id = "d1")
    private val useCase = GetDishUseCase(FakeDishRepository(listOf(stored)))

    @Test
    fun `existing dish returns Success`() = runBlocking {
        assertEquals(Outcome.Success(stored), useCase("d1"))
    }

    @Test
    fun `missing dish returns DishNotFound`() = runBlocking {
        assertEquals(Outcome.Failure(DomainError.DishNotFound), useCase("missing"))
    }
}
