package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that DeleteDishUseCase blocks missing or planned dishes and deletes the rest.
 */
class DeleteDishUseCaseTest {

    @Test
    fun `missing dish fails with DishNotFound`() = runBlocking {
        val repository = FakeDishRepository()

        val result = DeleteDishUseCase(repository)("unknown")

        assertEquals(Outcome.Failure(DomainError.DishNotFound), result)
        assertTrue(repository.deletedIds.isEmpty())
    }

    @Test
    fun `dish used in calendar fails with DishInUse and is kept`() = runBlocking {
        val repository = FakeDishRepository(listOf(dish(id = "d1")))
        repository.usedInCalendar += "d1"

        val result = DeleteDishUseCase(repository)("d1")

        assertEquals(Outcome.Failure(DomainError.DishInUse), result)
        assertTrue(repository.deletedIds.isEmpty())
        assertNotNull(repository.current["d1"])
    }

    @Test
    fun `unused dish is deleted`() = runBlocking {
        val repository = FakeDishRepository(listOf(dish(id = "d1"), dish(id = "d2")))

        val result = DeleteDishUseCase(repository)("d1")

        assertEquals(Outcome.Success(Unit), result)
        assertEquals(listOf("d1"), repository.deletedIds)
        assertFalse("d1" in repository.current)
        assertTrue("d2" in repository.current)
    }
}
