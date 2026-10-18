package com.abhinav.kotlinytmusicscraper

import com.abhinav.kotlinytmusicscraper.models.*
import com.abhinav.kotlinytmusicscraper.pages.SearchPage
import com.abhinav.kotlinytmusicscraper.pages.SearchSuggestionPage
import com.abhinav.kotlinytmusicscraper.pages.SearchSummaryPage
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Reduced YouTube renderer fixtures, exercised through public search page parsers. */
class SearchSongMetadataTest {
    private val artistId = "UC1234567890123456789012"
    private val guestId = "UCabcdefghijklmnopqrstuv"
    private val albumId = "MPREb_album"
    private val wire = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val base = wire.decodeFromString<MusicResponsiveListItemRenderer>(searchRowJson)

    @Test fun fieldMeaningDoesNotDependOnSongLabelOrSubtitlePosition() {
        for (prefix in listOf(emptyList(), listOf(Run("Song", null), Run(" • ", null)))) {
            val renderer = fixture(prefix + byline(" • "))
            songsFromEveryPath(renderer).forEach(::assertSong)
        }
    }

    @Test fun unicodeSeparatorsDoNotSplitLinkedArtistNames() {
        for (separator in listOf("\u00A0\u2022\u202F", " \u00B7 ", "\u2219", "\u2027")) {
            val runs = byline(separator)
            assertEquals(listOf("Imagine Dragons", "Evolve", "3:25"), runs.splitBySeparator().map { it.single().text })
            songsFromEveryPath(fixture(runs)).forEach(::assertSong)
        }
        val linked = endpointRun("Earth • Wind & Fire", artistId, "MUSIC_PAGE_TYPE_ARTIST")
        assertEquals(listOf(listOf(linked)), listOf(linked).splitBySeparator())
    }

    @Test fun endpointsRetainMultipleArtistsAndIgnoreOtherBrowseTargets() {
        val metadata = listOf(
            endpointRun("Evolve", albumId, "MUSIC_PAGE_TYPE_ALBUM"),
            endpointRun("Earth, Wind & Fire", artistId, "MUSIC_PAGE_TYPE_ARTIST"),
            Run(", ", null), endpointRun("Guest channel", guestId, "MUSIC_PAGE_TYPE_USER_CHANNEL"),
            endpointRun("Duplicate", artistId, "MUSIC_PAGE_TYPE_ARTIST"),
            endpointRun("Other collection", "VL_other", "MUSIC_PAGE_TYPE_PLAYLIST"),
            Run("1.5M views", null), Run("3:25", null),
        ).songByline()
        assertEquals(listOf(Artist("Earth, Wind & Fire", artistId), Artist("Guest channel", guestId)), metadata.artists)
        assertEquals(Album("Evolve", albumId), metadata.album)
        assertEquals(205, metadata.duration)
    }

    @Test fun fixedColumnDurationAndNumericLinkedAlbumTitleAreHandled() {
        val renderer = fixture(listOf(endpointRun("Imagine Dragons", artistId, "MUSIC_PAGE_TYPE_ARTIST"),
            endpointRun("3:25", albumId, "MUSIC_PAGE_TYPE_ALBUM")))
            .copy(fixedColumns = listOf(column(listOf(Run("1:02:03", null)))))
        listOf(SearchPage.toYTItem(renderer), SearchSummaryPage.fromMusicResponsiveListItemRenderer(renderer),
            SearchSuggestionPage.fromMusicResponsiveListItemRenderer(renderer)).forEach {
            val song = assertIs<SongItem>(it)
            assertEquals(3723, song.duration)
            assertEquals("3:25", song.album?.name)
        }
        assertNull(listOf(endpointRun("3:25", albumId, "MUSIC_PAGE_TYPE_ALBUM")).songByline().duration)
    }

    @Test fun invalidAndZeroDurationsDoNotBecomeMetadata() {
        for (label in listOf("3:99", "3:25:99", "-1:25", "0:00", "1.5M views", "live")) {
            assertNull(listOf(Run(label, null)).songByline().duration)
        }
        assertEquals(205, listOf(Run("3.25", null)).songByline().duration)
        val renderer = base.copy(flexColumns = base.flexColumns.take(1), fixedColumns = listOf(column(listOf(Run("3:25", null)))))
        assertEquals(205, assertIs<SongItem>(SearchPage.toYTItem(renderer)).duration)
    }

    @Test fun typedVideoSuggestionKeepsItsNewMusicVideoType() {
        val video = fixture(byline(" • ")).copy(navigationEndpoint = NavigationEndpoint(watchEndpoint = WatchEndpoint(
            videoId = "believer", watchEndpointMusicSupportedConfigs = WatchEndpoint.WatchEndpointMusicSupportedConfigs(
                WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig("MUSIC_VIDEO_TYPE_OMV")),
        )))
        val suggestion = assertIs<VideoItem>(SearchSuggestionPage.fromMusicResponsiveListItemRenderer(video))
        assertEquals("MUSIC_VIDEO_TYPE_OMV", suggestion.musicVideoType)
        assertEquals(listOf(Artist("Imagine Dragons", artistId)), suggestion.artists)
        assertEquals(Album("Evolve", albumId), suggestion.album)
        assertEquals(205, suggestion.duration)
    }

    @Test fun missingPageTypeFallsBackOnlyToRecognizableBrowseIds() {
        val metadata = listOf(Run("Artist", NavigationEndpoint(browseEndpoint = BrowseEndpoint(artistId))),
            Run("Album", NavigationEndpoint(browseEndpoint = BrowseEndpoint(albumId))),
            endpointRun("Not an artist", guestId, "MUSIC_PAGE_TYPE_PLAYLIST"),
            Run("Unlinked label", null)).songByline()
        assertEquals(listOf(Artist("Artist", artistId)), metadata.artists)
        assertEquals(Album("Album", albumId), metadata.album)
    }

    private fun songsFromEveryPath(renderer: MusicResponsiveListItemRenderer): List<SongItem> {
        val card = MusicCardShelfRenderer(title = renderer.flexColumns.first().musicResponsiveListItemFlexColumnRenderer.text!!,
            subtitle = renderer.flexColumns[1].musicResponsiveListItemFlexColumnRenderer.text!!, thumbnail = renderer.thumbnail!!,
            header = MusicCardShelfRenderer.Header(MusicCardShelfRenderer.Header.MusicCardShelfHeaderBasicRenderer(Runs(emptyList()))),
            contents = null, buttons = emptyList(), onTap = NavigationEndpoint(watchEndpoint = WatchEndpoint(videoId = "believer")), subtitleBadges = null)
        return listOf(SearchPage.toYTItem(renderer), SearchSummaryPage.fromMusicResponsiveListItemRenderer(renderer),
            SearchSuggestionPage.fromMusicResponsiveListItemRenderer(renderer), SearchSummaryPage.fromMusicCardShelfRenderer(card)).map { assertIs<SongItem>(it) }
    }
    private fun assertSong(song: SongItem) {
        assertEquals("believer", song.id)
        assertEquals("Believer", song.title)
        assertEquals(listOf(Artist("Imagine Dragons", artistId)), song.artists)
        assertEquals(Album("Evolve", albumId), song.album)
        assertEquals(205, song.duration)
    }
    private fun byline(separator: String) = listOf(endpointRun("Imagine Dragons", artistId, "MUSIC_PAGE_TYPE_ARTIST"),
        Run(separator, null), endpointRun("Evolve", albumId, "MUSIC_PAGE_TYPE_ALBUM"), Run(separator, null), Run("3:25", null))
    private fun endpointRun(text: String, id: String, type: String) = Run(text, NavigationEndpoint(browseEndpoint = BrowseEndpoint(id,
        browseEndpointContextSupportedConfigs = BrowseEndpoint.BrowseEndpointContextSupportedConfigs(
            BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig(type)))))
    private fun column(runs: List<Run>) = MusicResponsiveListItemRenderer.FlexColumn(
        MusicResponsiveListItemRenderer.FlexColumn.MusicResponsiveListItemFlexColumnRenderer(Runs(runs)))
    private fun fixture(runs: List<Run>) = base.copy(flexColumns = listOf(base.flexColumns.first(), column(runs)))
}

private const val searchRowJson = """
{
  "flexColumns": [
    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [
      {"text": "Believer", "navigationEndpoint": {"watchEndpoint": {"videoId": "believer"}}}
    ]}}},
    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [
      {"text": "Imagine Dragons", "navigationEndpoint": {"browseEndpoint": {"browseId": "UC1234567890123456789012", "browseEndpointContextSupportedConfigs": {"browseEndpointContextMusicConfig": {"pageType": "MUSIC_PAGE_TYPE_ARTIST"}}}}},
      {"text": " • "},
      {"text": "Evolve", "navigationEndpoint": {"browseEndpoint": {"browseId": "MPREb_album", "browseEndpointContextSupportedConfigs": {"browseEndpointContextMusicConfig": {"pageType": "MUSIC_PAGE_TYPE_ALBUM"}}}}},
      {"text": " • "}, {"text": "3:25"}
    ]}}}
  ],
  "fixedColumns": [{"musicResponsiveListItemFixedColumnRenderer": {"text": {"runs": [{"text": "3:25"}]}}}],
  "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://image.example/believer.jpg", "width": 120, "height": 120}]}}}
}
"""
