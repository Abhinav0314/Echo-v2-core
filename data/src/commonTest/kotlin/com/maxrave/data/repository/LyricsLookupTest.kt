package com.maxrave.data.repository

import com.maxrave.domain.utils.Resource
import com.maxrave.domain.data.model.metadata.Lyrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.simpmusic.lyrics.parser.parseSyncedLyrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class LyricsLookupTest {
    @Test
    fun successfulNullSearchEmitsErrorInsteadOfSilentlySkippingOfflineFallback(): Unit = runBlocking {
        assertIs<Resource.Error<*>>(lrclibLyricsFlow { Result.success(null) }.single())
    }

    @Test
    fun emptyParsedSearchAlsoEmitsError(): Unit = runBlocking {
        assertIs<Resource.Error<*>>(lrclibLyricsFlow { Result.success(parseSyncedLyrics("not an LRC line")) }.single())
    }

    @Test
    fun ordinarySyncedLyricsReachTheSharedPlayerDisplayModel(): Unit = runBlocking {
        val response = lrclibLyricsFlow {
            Result.success(parseSyncedLyrics("[00:01.123]First line\n[00:02.456]Second line"))
        }.single()
        val success = assertIs<Resource.Success<Lyrics>>(response)
        val lyrics = requireNotNull(success.data)
        val lines = requireNotNull(lyrics.lines)
        assertEquals("LINE_SYNCED", lyrics.syncType)
        assertEquals(listOf("First line", "Second line"), lines.map { it.words })
        assertEquals(listOf("1123", "2456"), lines.map { it.startTimeMs })
    }

    @Test
    fun providerFailureIsAnErrorButCancellationPropagates(): Unit = runBlocking {
        assertIs<Resource.Error<*>>(lrclibLyricsFlow { Result.failure(Exception("HTTP 403")) }.single())
        assertFailsWith<CancellationException> {
            lrclibLyricsFlow { Result.failure(CancellationException("track changed")) }.single()
        }
    }
}
