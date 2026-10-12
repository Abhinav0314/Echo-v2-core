package com.maxrave.data.mapping

import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.songs.Album
import com.maxrave.domain.data.model.searchResult.songs.Artist
import kotlinx.serialization.json.Json
import com.maxrave.domain.utils.toTrack
import com.maxrave.domain.utils.toSongEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackMetadataPatchTest {
    private val channelId = "UC1234567890123456789012"

    @Test
    fun freshSelectionRepairsOnlyVerifiedArtistAlbumAndPositiveDuration() {
        val patch = track(
            artists = listOf(Artist(channelId, "Imagine Dragons"), Artist("MPRE_album", "Evolve"), Artist(null, "3:25")),
            album = Album("MPRE_album", "Evolve"), seconds = 205,
        ).metadataPatch()
        assertEquals(listOf(channelId), Json.decodeFromString<List<String>>(patch.artistIdsJson!!))
        assertEquals(listOf("Imagine Dragons"), Json.decodeFromString<List<String>>(patch.artistNamesJson!!))
        assertEquals("MPRE_album", patch.albumId)
        assertEquals("Evolve", patch.albumName)
        assertEquals(205, patch.durationSeconds)
        assertEquals("3:25", patch.duration)
    }

    @Test
    fun missingUnlinkedOrMalformedMetadataDoesNotClearPersistedFields() {
        listOf(0, -1).forEach { seconds ->
            val patch = track(listOf(Artist(null, "Author"), Artist("VL_playlist", "Collection"), Artist("UC_short", "Unverified")),
                Album("", "Unlinked album"), seconds).metadataPatch()
            assertTrue(patch.isEmpty)
            assertNull(patch.artistNamesJson)
            assertNull(patch.albumName)
            assertNull(patch.duration)
        }
        val nameless = track(listOf(Artist(channelId, " ")), Album("MPRE_album", " "), null).metadataPatch()
        assertTrue(nameless.isEmpty)
    }

    @Test
    fun validPartialMetadataUpdatesIndependentlyAndLongDurationIsFormatted() {
        val patch = track(emptyList(), null, 3723).metadataPatch()
        assertNull(patch.artistIdsJson)
        assertNull(patch.albumId)
        assertEquals("1:02:03", patch.duration)
        assertEquals(3723, patch.durationSeconds)
    }

    @Test fun collaborationArraysRemainPairedWithoutSplittingDisplayNames() {
        val guest = "UCabcdefghijklmnopqrstuv"
        val patch = track(listOf(Artist(channelId, "Earth, Wind & Fire"), Artist(guest, "Guest"), Artist(channelId, "Duplicate")), null, null).metadataPatch()
        assertEquals(listOf(channelId, guest), Json.decodeFromString<List<String>>(patch.artistIdsJson!!))
        assertEquals(listOf("Earth, Wind & Fire", "Guest"), Json.decodeFromString<List<String>>(patch.artistNamesJson!!))
    }

    @Test fun aChannelCannotOverwriteTheAlbumAndJaggedCachedIdsRemainReadable() {
        assertNull(track(emptyList(), Album(channelId, "Artist"), null).metadataPatch().albumId)
        val cached = track(emptyList(), null, 205).toSongEntity().copy(artistId = listOf(channelId), artistName = listOf("Main", "Guest"),
            liked = true, totalPlayTime = 1234, downloadState = 3, canvasUrl = "saved-canvas")
        val recovered = cached.toTrack()
        assertEquals(listOf("Main", "Guest"), recovered.artists?.map { it.name })
        assertEquals(listOf(channelId, ""), recovered.artists?.map { it.id })
    }

    private fun track(artists: List<Artist>?, album: Album?, seconds: Int?) = Track(
        videoId = "believer", title = "Believer", artists = artists, album = album,
        duration = "untrusted duration label", durationSeconds = seconds,
        isAvailable = true, isExplicit = false, likeStatus = null, thumbnails = null,
        videoType = null, category = null, feedbackTokens = null, resultType = null,
    )
}
