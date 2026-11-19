package com.superdenot.powerguard.ui.viewmodel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PowerGuardViewModelCancellationTest {
    @Test
    fun executeSuspendFailClosed_propagatesCancellationException() = runTest {
        val cancellation = CancellationException("test cancellation")
        var fallbackCalled = false
        val thrown = try {
            executeSuspendFailClosed(
                operation = { throw cancellation },
                onFailure = { fallbackCalled = true; "fallback" }
            )
            null
        } catch (error: CancellationException) {
            error
        }
        assertSame(cancellation, thrown)
        assertEquals(false, fallbackCalled)
    }

    @Test
    fun executeSuspendFailClosed_usesFailClosedFallbackForOperationalException() = runTest {
        val result = executeSuspendFailClosed(
            operation = { throw IllegalStateException("storage unavailable") },
            onFailure = { "disabled" }
        )
        assertEquals("disabled", result)
    }
}
