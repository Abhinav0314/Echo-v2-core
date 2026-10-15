package com.maxrave.data.mapping

import org.simpmusic.lyrics.models.response.LyricsResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LyricsMappingTest {
    @Test
    fun validThreeDigitLrcResponseReachesTheSharedDisplayModel() {
        val lyrics = response(synced = "[00:01.123]First line\n[00:02.456]Second line").toLyrics()!!
        assertEquals("LINE_SYNCED", lyrics.syncType)
        assertEquals(listOf("First line", "Second line"), lyrics.lines!!.map { it.words })
        assertEquals(listOf("1123", "2456"), lyrics.lines!!.map { it.startTimeMs })
    }

    @Test
    fun malformedRichLyricsCannotHideUsableSyncedLyrics() {
        val lyrics = response(rich = "unsupported rich text", synced = "[00:01.23]Visible line").toLyrics()!!
        assertEquals("LINE_SYNCED", lyrics.syncType)
        assertEquals("Visible line", lyrics.lines!!.single().words)
    }

    @Test
    fun malformedTimedLyricsFallBackToPlainText() {
        val lyrics = response(rich = "unsupported rich text", synced = "unsupported timed text", plain = "Visible plain line").toLyrics()!!
        assertEquals("UNSYNCED", lyrics.syncType)
        assertEquals("Visible plain line", lyrics.lines!!.single().words)
    }

    @Test
    fun emptyResponsesCannotBecomeSuccessfulBlankLyrics() {
        assertNull(response(rich = "unsupported rich text", synced = "unsupported timed text").toLyrics())
    }

    private fun response(rich: String? = null, synced: String? = null, plain: String = "") = LyricsResponse(
        id = "fixture", videoId = "track", songTitle = "Track", artistName = "Artist", albumName = "Album",
        durationSeconds = 120, plainLyric = plain, syncedLyrics = synced, richSyncLyrics = rich,
        trackType = null, vote = 0, contributor = "", contributorEmail = "",
    )
}
