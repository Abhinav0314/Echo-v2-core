package org.simpmusic.lyrics

import kotlinx.coroutines.runBlocking
import org.simpmusic.lyrics.models.request.LyricsBody
import org.simpmusic.lyrics.models.request.TranslatedLyricsBody
import kotlin.test.*

class DisabledCommunityProviderTest {
    @Test fun disabledProviderCannotIssueLookupsTranslationsVotesOrContributions(): Unit = runBlocking {
        val client = SimpMusicLyricsClient(communityApiEnabled = false)
        val results = listOf(
            client.getLyrics("example"),
            client.getTranslatedLyrics("example", "en"),
            client.voteLyrics("example", true),
            client.voteTranslatedLyrics("example", true),
            client.insertLyrics(LyricsBody("example", "Title", "Artist", "Album", 180, "Lyrics", trackType = "song", contributor = "Example", contributorEmail = "example@example.com")),
            client.insertTranslatedLyrics(TranslatedLyricsBody("example", "Lyrics", "en", "Example", "example@example.com")),
        )
        results.forEach { assertIs<LyricsProviderDisabledException>(it.exceptionOrNull()) }
    }
}
