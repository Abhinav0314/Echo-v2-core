package org.simpmusic.lyrics.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class LrcTextParserTest {
    @Test
    fun millisecondTimestampsProduceVisibleLines() {
        val lyrics = requireNotNull(parseSyncedLyrics("[00:01.123]First line\n[00:02.456]Second line").lyrics)
        val lines = requireNotNull(lyrics.lines)
        assertEquals("LINE_SYNCED", lyrics.syncType)
        assertEquals(listOf("First line", "Second line"), lines.map { it.words })
        assertEquals(listOf("1123", "2456"), lines.map { it.startTimeMs })
    }

    @Test
    fun centisecondsAreScaledToMillisecondsWithoutDroppingFirstCharacter() {
        val line = requireNotNull(requireNotNull(parseSyncedLyrics("[00:27.12]boy &amp; girl").lyrics).lines).single()
        assertEquals("27120", line.startTimeMs)
        assertEquals("boy & girl", line.words)
    }

    @Test
    fun tenthsWholeSecondsAndSingleDigitMinutesAreAccepted() {
        val lines = requireNotNull(requireNotNull(parseSyncedLyrics("[0:02.5]One\n[1:03]Two").lyrics).lines)
        assertEquals(listOf("2500", "63000"), lines.map { it.startTimeMs })
        assertEquals(listOf("One", "Two"), lines.map { it.words })
    }

    @Test
    fun repeatedTimestampTagsProduceOrderedLinesAndSkipMetadata() {
        val lines = requireNotNull(requireNotNull(parseSyncedLyrics("[ar:Example]\n[00:20.00][00:10.00]Repeated line\n[00:30.00]").lyrics).lines)
        assertEquals(listOf("10000", "20000", "30000"), lines.map { it.startTimeMs })
        assertEquals(listOf("Repeated line", "Repeated line", "♫"), lines.map { it.words })
    }
}
