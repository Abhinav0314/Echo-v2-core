package com.abhinav.data.parser

import com.abhinav.kotlinytmusicscraper.models.BrowseEndpoint
import com.abhinav.kotlinytmusicscraper.models.GridRenderer
import com.abhinav.kotlinytmusicscraper.models.MusicTwoRowItemRenderer
import com.abhinav.kotlinytmusicscraper.models.NavigationEndpoint
import com.abhinav.kotlinytmusicscraper.models.Run
import com.abhinav.kotlinytmusicscraper.models.Runs
import com.abhinav.kotlinytmusicscraper.models.ThumbnailRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryPlaylistParserTest {
    @Test
    fun absentAndEmptyTextRunsKeepPlaylistIdAndFallbackMetadata() {
        for (runs in listOf<List<Run>?>(null, emptyList())) {
            val renderer = renderer(runs = runs)
            val first = parseLibraryPlaylist(listOf(GridRenderer.Item(null, renderer))).single()
            val next = parseNextLibraryPlaylist(listOf(renderer)).single()
            for (playlist in listOf(first, next)) {
                assertEquals("VLsaved-playlist", playlist.browseId)
                assertEquals("", playlist.title)
                assertEquals("", playlist.author)
                assertTrue(playlist.thumbnails.isEmpty())
            }
        }
    }

    @Test
    fun browseFilteringAndPopulatedMetadataKeepTheirExistingBehavior() {
        val renderer = renderer(listOf(Run("Saved collection", null)))
        val first = parseLibraryPlaylist(listOf(GridRenderer.Item(null, renderer))).single()
        val next = parseNextLibraryPlaylist(listOf(renderer)).single()
        assertEquals(first, next)
        assertEquals("Saved collection", first.title)
        val withoutBrowse = renderer.copy(navigationEndpoint = NavigationEndpoint())
        assertTrue(parseLibraryPlaylist(listOf(GridRenderer.Item(null, withoutBrowse))).isEmpty())
        assertEquals("", parseNextLibraryPlaylist(listOf(withoutBrowse)).single().browseId)
    }

    private fun renderer(runs: List<Run>?) = MusicTwoRowItemRenderer(
        title = Runs(runs),
        subtitle = Runs(runs),
        menu = null,
        thumbnailRenderer = ThumbnailRenderer(null, null, null),
        navigationEndpoint = NavigationEndpoint(browseEndpoint = BrowseEndpoint("VLsaved-playlist")),
        thumbnailOverlay = null,
    )
}
