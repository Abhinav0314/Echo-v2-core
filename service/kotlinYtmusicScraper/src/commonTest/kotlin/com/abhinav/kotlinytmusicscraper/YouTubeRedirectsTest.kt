package com.abhinav.kotlinytmusicscraper

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class YouTubeRedirectsTest {
    @Test
    fun cleartextRedirectIsNeverRequested() = runBlocking {
        val requests = mutableListOf<String>()
        val engine = MockEngine { request ->
            requests += request.url.toString()
            respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "http://cdn.example.test/song"))
        }
        HttpClient(engine) { installSecureYouTubeRedirects() }.use { client ->
            assertEquals(HttpStatusCode.Found, client.get("https://music.example.test/song").status)
            assertEquals(listOf("https://music.example.test/song"), requests)
        }
    }

    @Test
    fun secureCdnRedirectStillDownloads() = runBlocking {
        val requests = mutableListOf<String>()
        val engine = MockEngine { request ->
            requests += request.url.toString()
            if (request.url.host == "music.example.test") {
                respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://cdn.example.test/song"))
            } else {
                respond("audio", HttpStatusCode.OK)
            }
        }
        HttpClient(engine) { installSecureYouTubeRedirects() }.use { client ->
            assertEquals("audio", client.get("https://music.example.test/song").bodyAsText())
            assertEquals(listOf("https://music.example.test/song", "https://cdn.example.test/song"), requests)
        }
    }
}
