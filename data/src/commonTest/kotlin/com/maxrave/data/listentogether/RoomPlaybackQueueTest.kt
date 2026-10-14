package com.maxrave.data.listentogether

import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.listentogether.ListenTogetherRoom
import com.maxrave.domain.data.model.listentogether.RoomConnection
import com.maxrave.domain.data.model.listentogether.RoomTrack
import com.maxrave.domain.data.model.searchResult.songs.Album
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.mediaservice.handler.QueueData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull

class RoomPlaybackQueueTest {
    @Test
    fun hostQueueUsesContainerPayloadAndPreservesPlaybackOrderAndMetadata() {
        val queue = QueueData(
            queueState = QueueData.StateSource.STATE_INITIALIZED,
            data = QueueData.Data(listTracks = listOf(track("current"), track("next")), playlistName = "Night drive"),
        ).toRoomPlaybackQueue()
        assertEquals(listOf("current", "next"), queue.tracks.map { it.id })
        assertEquals("Night drive", queue.title)
        val current = queue.tracks.first()
        assertEquals("Song current", current.title)
        assertEquals("Main, Guest", current.artist)
        assertEquals("Album", current.album)
        assertEquals(205_000L, current.duration)
        assertEquals("large-art", current.thumbnail)
    }

    @Test
    fun replacingAndClearingUpcomingTracksProducesNewPublishableSnapshots() {
        val first = QueueData(data = QueueData.Data(listTracks = listOf(track("current"), track("old-next")), playlistName = "First"))
        val replaced = first.copy(data = first.data.copy(listTracks = listOf(track("current"), track("new-next"))))
        val cleared = replaced.copy(data = replaced.data.copy(listTracks = emptyList()))
        val snapshots = listOf(first, replaced, cleared).map { it.toRoomPlaybackQueue() }.distinct()
        assertEquals(listOf(listOf("current", "old-next"), listOf("current", "new-next"), emptyList()),
            snapshots.map { it.tracks.map { track -> track.id } })
        assertEquals("First", snapshots.last().title)
    }

    @Test
    fun renamedQueueIsNotDroppedEvenWhenItsTrackIdsAreUnchanged() {
        val first = QueueData(data = QueueData.Data(listTracks = listOf(track("current")), playlistName = "Original"))
        val renamed = first.copy(data = first.data.copy(playlistName = "Renamed"))
        assertEquals(2, listOf(first, renamed).map { it.toRoomPlaybackQueue() }.distinct().size)
        assertEquals("Renamed", renamed.toRoomPlaybackQueue().title)
    }

    @Test
    fun absentQueueAndIncompleteTrackMetadataRemainSafeForJoinPublication() {
        assertEquals(emptyList(), (null as QueueData?).toRoomPlaybackQueue().tracks)
        assertEquals("", QueueData().toRoomPlaybackQueue().title)
        val incomplete = track("local").copy(artists = null, album = null, durationSeconds = null, thumbnails = null)
        val published = QueueData(data = QueueData.Data(listTracks = listOf(incomplete))).toRoomPlaybackQueue().tracks.single()
        assertEquals("local", published.id)
        assertEquals("", published.artist)
        assertEquals("", published.album)
        assertEquals("", published.thumbnail)
        assertEquals(0L, published.duration)
    }

    @Test
    fun delayedPlayCanOnlyBePublishedByTheOriginalHostForItsCurrentTrack() {
        val hosting = ListenTogetherRoom(roomCode = "ROOM-A", selfUserId = "host-a", isHost = true)
        assertTrue(mayPublishSettledPlayback(hosting, hosting, "track-a", "track-a"))
        assertFalse(mayPublishSettledPlayback(hosting, hosting.copy(roomCode = null), "track-a", "track-a"))
        assertFalse(mayPublishSettledPlayback(hosting, hosting.copy(roomCode = "ROOM-B"), "track-a", "track-a"))
        assertFalse(mayPublishSettledPlayback(hosting, hosting.copy(isHost = false), "track-a", "track-a"))
        assertFalse(mayPublishSettledPlayback(hosting, hosting.copy(selfUserId = "host-b"), "track-a", "track-a"))
        assertFalse(mayPublishSettledPlayback(hosting, hosting, "track-a", "track-b"))
        assertFalse(mayPublishSettledPlayback(hosting, hosting, "track-a", null))
    }

    @Test
    fun guestQueueStartsWithCurrentTrackAndReplacesOldCatalogWithoutContinuation() {
        val current = RoomTrack("current", "Now", "Host artist", durationMs = 205_999L, thumbnail = "host-art")
        val next = RoomTrack("next", "Next", "Guest artist")
        val ordered = roomPlaybackOrder(current, listOf(next, current, next, current.copy(id = " ")))
        assertEquals(listOf("current", "next"), ordered.map { it.id })
        val data = ordered.toGuestQueueData("Host queue")
        assertEquals(listOf("current", "next"), data.listTracks.map { it.videoId })
        assertEquals(data.listTracks.first(), data.firstPlayedTrack)
        assertEquals("Host queue", data.playlistName)
        assertNull(data.playlistId)
        assertNull(data.continuation)
        assertEquals(205, data.listTracks.first().durationSeconds)
        assertEquals("Host artist", data.listTracks.first().artists?.single()?.name)
        assertEquals("host-art", data.listTracks.first().thumbnails?.single()?.url)
        assertEquals("MUSIC_VIDEO_TYPE_ATV", data.listTracks.first().videoType)
        assertNull(data.listTracks.last().album)
        assertNull(data.listTracks.last().thumbnails)
    }

    @Test
    fun bufferReadinessRequiresTheRoomTrackAndCanArriveAfterTheInitialRoomEvent() {
        val room = ListenTogetherRoom(
            connection = RoomConnection.Connected("1"), roomCode = "ROOM", selfUserId = "guest",
            currentTrack = RoomTrack("new-song", "Next", "Artist"), waitingFor = listOf("guest"),
        )
        assertFalse(mayReportRoomBufferReady(room, "old-song", 100))
        assertFalse(mayReportRoomBufferReady(room, "new-song", 0))
        assertTrue(mayReportRoomBufferReady(room, "new-song", 5))
        assertFalse(mayReportRoomBufferReady(room.copy(waitingFor = emptyList()), "new-song", 100))
        assertFalse(mayReportRoomBufferReady(room.copy(roomCode = null), "new-song", 100))
        assertFalse(mayReportRoomBufferReady(room.copy(connection = RoomConnection.Connecting), "new-song", 100))
    }

    private fun track(id: String) = Track(
        album = Album("album", "Album"), artists = listOf(Artist("main", "Main"), Artist("guest", "Guest")),
        duration = "3:25", durationSeconds = 205, isAvailable = true, isExplicit = false, likeStatus = null,
        thumbnails = listOf(Thumbnail(64, "small-art", 64), Thumbnail(512, "large-art", 512)),
        title = "Song $id", videoId = id, videoType = null, category = null, feedbackTokens = null, resultType = null,
    )
}
