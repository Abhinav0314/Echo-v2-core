package com.abhinav.domain.utils

import com.abhinav.domain.data.model.taste.TasteException
import kotlin.test.*

class TasteErrorPrivacyTest {
    @Test fun providerExceptionsRetainCategoryWithoutDisplayingPrivateDetails() {
        TasteException.Kind.entries.forEach { kind ->
            val cause=IllegalStateException("private@example.com api-key=secret https://private.invalid")
            val failure=TasteException(kind,403,cause)
            assertEquals(kind,failure.kind)
            assertEquals(403,failure.statusCode)
            assertSame(cause,failure.cause)
            assertFalse(failure.message.orEmpty().contains("private@example.com"))
            assertFalse(failure.message.orEmpty().contains("api-key=secret"))
            assertFalse(failure.message.orEmpty().contains("https://"))
            assertTrue(failure.message.orEmpty().isNotBlank())
        }
    }
}
