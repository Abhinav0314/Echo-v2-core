package org.abhinav.echo.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LyricsRequestTest {
    @Test
    fun blockedHtmlIsReportedAsHttp403BeforeAnyJsonDecode(): Unit = runBlocking {
        val engine = MockEngine {
            respond("<html>Sorry, you have been blocked</html>", HttpStatusCode.Forbidden, headersOf(HttpHeaders.ContentType, "text/html"))
        }
        HttpClient(engine) { install(ContentNegotiation) { json() } }.use { client ->
            val result = lyricsResult { client.get("https://lyrics.example.test/rJ0D1GbDq1Q").lyricsBodyOrThrow<List<String>>() }
            val failure = assertIs<SimpMusicLyricsApiException>(result.exceptionOrNull())
            assertEquals(403, failure.code)
            assertEquals("Lyrics provider returned HTTP 403 (text/html)", failure.message)
            assertFalse(requireNotNull(failure.message).contains("BaseResponse"))
        }
    }

    @Test
    fun successfulJsonStillDecodesTheNormalProviderEnvelope(): Unit = runBlocking {
        val engine = MockEngine {
            respond("""{"success":true,"data":["fixture"]}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json; charset=utf-8"))
        }
        HttpClient(engine) { install(ContentNegotiation) { json() } }.use { client ->
            assertEquals(listOf("fixture"), client.get("https://lyrics.example.test/track").lyricsBodyOrThrow<List<String>>())
        }
    }

    @Test
    fun stalledProviderReturnsARequestFailureEligibleForFallback(): Unit = runBlocking {
        val engine = MockEngine {
            delay(10_000L)
            respond("never reached")
        }
        HttpClient(engine) { installLyricsTimeouts(50L) }.use { client ->
            val result = lyricsResult { client.get("https://lyrics.example.test/track") }
            assertIs<HttpRequestTimeoutException>(result.exceptionOrNull())
        }
    }

    @Test
    fun cancellationCannotBecomeAProviderFailure() {
        assertFailsWith<CancellationException> {
            lyricsResult<String> { throw CancellationException("track changed") }
        }
    }
}
