package com.gabrieltagama.menuplanner.core.domain.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests of the Outcome operators: steps after a failure never run and the failure travels unchanged,
 * fold picks the branch of each case and the side actions run only for their own case.
 */
class OutcomeTest {
    private val success: Outcome<Int> = Outcome.Success(2)
    private val failure: Outcome<Int> = Outcome.Failure(DomainError.DishNotFound)

    @Test
    fun `map transforms a success and keeps a failure`() {
        assertEquals(Outcome.Success(4), success.map { it * 2 })
        assertEquals(failure, failure.map { error("must not run") })
    }

    @Test
    fun `flatMap chains a success and keeps a failure`() {
        assertEquals(Outcome.Failure(DomainError.DishInUse), success.flatMap { Outcome.Failure(DomainError.DishInUse) })
        assertEquals(Outcome.Success("2"), success.flatMap { Outcome.Success(it.toString()) })
        assertEquals(failure, failure.flatMap<Int, Int> { error("must not run") })
    }

    @Test
    fun `fold picks the branch of each case`() {
        assertEquals("ok 2", success.fold({ "ok $it" }, { "ko $it" }))
        assertEquals("ko DishNotFound", failure.fold({ "ok $it" }, { "ko $it" }))
    }

    @Test
    fun `side actions run only for their own case`() {
        val seen = mutableListOf<String>()
        success.onSuccess { seen += "success $it" }.onFailure { seen += "failure" }
        failure.onSuccess { seen += "success" }.onFailure { seen += "failure $it" }

        assertEquals(listOf("success 2", "failure DishNotFound"), seen)
    }

    @Test
    fun `getOrNull drops the error`() {
        assertEquals(2, success.getOrNull())
        assertNull(failure.getOrNull())
    }
}
