package com.maxrave.kotlinytmusicscraper

import com.maxrave.kotlinytmusicscraper.utils.buildYouTubeAuthorizationHeader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YouTubeAuthorizationTest {
    private val timestamp = 1_700_000_000L

    @Test
    fun absentOrEmptySessionCookiesDoNotSendAuthorization() {
        assertNull(buildYouTubeAuthorizationHeader(emptyMap(), timestamp))
        assertNull(buildYouTubeAuthorizationHeader(mapOf("OTHER" to "value"), timestamp))
        assertNull(buildYouTubeAuthorizationHeader(mapOf("SAPISID" to "", "__Secure-3PAPISID" to ""), timestamp))
    }

    @Test
    fun eitherSessionCookieCanAuthorizeTheRequest() {
        val expected = "SAPISIDHASH 1700000000_aaf0864ffca316a10fd96b3ad740f139eb86f0d9"
        assertEquals(expected, buildYouTubeAuthorizationHeader(mapOf("SAPISID" to "session-value"), timestamp))
        assertEquals(expected, buildYouTubeAuthorizationHeader(mapOf("__Secure-3PAPISID" to "session-value"), timestamp))
        assertEquals(expected, buildYouTubeAuthorizationHeader(mapOf("SAPISID" to "", "__Secure-3PAPISID" to "session-value"), timestamp))
    }

    @Test
    fun sapisidHasPriorityWhenBothSessionCookiesExist() {
        assertEquals(
            buildYouTubeAuthorizationHeader(mapOf("SAPISID" to "session-value"), timestamp),
            buildYouTubeAuthorizationHeader(mapOf("SAPISID" to "session-value", "__Secure-3PAPISID" to "other-value"), timestamp),
        )
    }

    @Test
    fun headerPreservesTheUnixSecondsTimestamp() {
        val header = buildYouTubeAuthorizationHeader(mapOf("SAPISID" to "session-value"), timestamp)
        assertEquals("1700000000", header?.substringAfter("SAPISIDHASH ")?.substringBefore('_'))
    }
}
