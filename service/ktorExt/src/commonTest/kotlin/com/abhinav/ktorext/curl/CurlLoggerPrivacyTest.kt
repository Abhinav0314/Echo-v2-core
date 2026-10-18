package com.abhinav.ktorext.curl

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.takeFrom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CurlLoggerPrivacyTest {
    @Test fun credentialsInEveryRequestFieldStayOutOfLogs() {
        val request = HttpRequestBuilder().apply {
            method = HttpMethod.Post
            url.takeFrom("https://private-user:private-password@music.example/private-path?token=private-query")
            headers.append("Authorization", "Bearer private-auth")
            headers.append("Cookie", "SID=private-cookie")
            headers.append("X-Api-Key", "private-key")
            setBody("{\"password\":\"private-body\"}")
        }
        val message = requestLogMessage(request)
        assertEquals("HTTP POST https://music.example", message)
        assertFalse(message.contains("private-"))
    }
}
