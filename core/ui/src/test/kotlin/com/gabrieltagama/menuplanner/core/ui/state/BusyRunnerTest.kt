package com.gabrieltagama.menuplanner.core.ui.state

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that BusyRunner runs one operation at a time and always releases the busy flag, also when
 * the operation fails.
 */
class BusyRunnerTest {
    private val scope = TestScope(StandardTestDispatcher())
    private val busyChanges = mutableListOf<Boolean>()
    private val runner = BusyRunner(scope) { busyChanges += it }

    @Test
    fun `a second request while running is ignored`() {
        val gate = CompletableDeferred<Unit>()
        var runs = 0
        runner.run {
            runs++
            gate.await()
        }
        runner.run { runs++ }
        scope.runCurrent()

        assertTrue(runner.isRunning)
        gate.complete(Unit)
        scope.runCurrent()

        assertEquals(1, runs)
        assertFalse(runner.isRunning)
        assertEquals(listOf(true, false), busyChanges)
    }

    @Test
    fun `busy flag is released when the operation fails`() {
        val dispatcher = StandardTestDispatcher()
        val failingRunner = BusyRunner(CoroutineScope(dispatcher + CoroutineExceptionHandler { _, _ -> })) { busyChanges += it }

        failingRunner.run { error("boom") }
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf(true, false), busyChanges)
        assertFalse(failingRunner.isRunning)
    }
}
