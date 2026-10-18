package com.abhinav.domain.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import kotlin.test.*

class UserErrorMessagesTest {
    private val privateText="private@example.com access_token=secret https://private.invalid internal_table"

    @Test fun arbitraryBackendDetailsAreNeverDisplayCopy() {
        val display=userErrorMessage(IllegalStateException(privateText,RuntimeException(privateText)))
        assertEquals("Something went wrong. Please try again.",display)
        assertFalse(display.contains("private@example.com"))
    }

    @Test fun allSharedLocalResourceWrappersDiscardRawFailures()=runBlocking {
        val streams=listOf(
            wrapMessageResource("Saved"){throw IllegalStateException(privateText)},
            wrapDataResource<String>{throw IllegalStateException(privateText)},
            wrapResultResource<String>{Result.failure(IllegalStateException(privateText))},
        )
        streams.forEach { stream ->
            val states=stream.toList()
            assertTrue(states.first() is LocalResource.Loading)
            assertTrue(states.last() is LocalResource.Error)
            assertEquals("Something went wrong. Please try again.",states.last().message)
        }
    }

    @Test fun cancellationDoesNotBecomeAnErrorToast()=runBlocking {
        val streams=listOf(
            wrapMessageResource("Saved"){throw CancellationException(privateText)},
            wrapDataResource<String>{throw CancellationException(privateText)},
            wrapResultResource<String>{Result.failure(CancellationException(privateText))},
        )
        streams.forEach { stream -> assertFailsWith<CancellationException>{stream.toList()} }
    }

    @Test fun requestScopedTimeoutHasFriendlyRecoveryCopy()=runBlocking {
        suspend fun timedOut(): Nothing=withTimeout(1){delay(100);error("not reached")}
        val timeout=runCatching{timedOut()}.exceptionOrNull()!!
        val streams=listOf(
            wrapMessageResource("Saved"){throw timeout},
            wrapDataResource<String>{throw timeout},
            wrapResultResource<String>{Result.failure(timeout)},
        )
        streams.forEach { stream ->
            assertEquals("This is taking longer than expected. Please try again.",stream.toList().last().message)
        }
    }
}
