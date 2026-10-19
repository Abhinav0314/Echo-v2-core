package com.abhinav.data.repository

import com.abhinav.domain.data.entities.analytics.query.SongPlaySample
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class SmartDownloadScoreTest {
    private val now = LocalDateTime.parse("2026-10-19T12:00:00")
    private fun play(id: String, time: String, duration: Long = 100, heard: Long = 100) =
        SongPlaySample(id, LocalDateTime.parse(time), duration, heard)

    @Test fun recentCompleteListensOutrankOlderAndSkippedPlays() {
        assertEquals(listOf("recent", "older", "skipped"), rankBySmartDownloadScore(listOf(
            play("older", "2026-10-05T12:00:00"),
            play("skipped", "2026-10-19T12:00:00", heard = 10),
            play("recent", "2026-10-19T12:00:00"),
        ), now))
    }

    @Test fun repeatedListeningAccumulatesAndDurationsAreBounded() {
        assertEquals(listOf("repeat", "overrun", "negative"), rankBySmartDownloadScore(listOf(
            play("repeat", "2026-10-19T12:00:00"),
            play("repeat", "2026-10-19T12:00:00"),
            play("overrun", "2026-10-19T12:00:00", heard = 10000),
            play("negative", "2026-10-19T12:00:00", heard = -100),
        ), now))
    }

    @Test fun equallyScoredSongsPreferTheLatestListenAndEmptyHistoryIsEmpty() {
        assertEquals(emptyList(), rankBySmartDownloadScore(emptyList(), now))
        assertEquals(listOf("latest", "old"), rankBySmartDownloadScore(listOf(
            play("old", "2026-10-05T12:00:00", duration = 0),
            play("latest", "2026-10-19T12:00:00", heard = 50),
        ), now))
    }
}
