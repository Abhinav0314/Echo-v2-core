package com.maxrave.data.listentogether

import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.data.model.listentogether.ListenTogetherRoom
import com.maxrave.domain.data.model.listentogether.RoomTrack
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.songs.Album
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import org.simpmusic.listentogether.TrackInfo

/** Snapshot of the actual queue container, not a cast to its unrelated nested payload type. */
internal data class RoomPlaybackQueue(
    val tracks: List<TrackInfo>,
    val title: String,
)

internal fun QueueData?.toRoomPlaybackQueue(): RoomPlaybackQueue =
    RoomPlaybackQueue(
        tracks = this?.data?.listTracks.orEmpty().map { track ->
            TrackInfo(
                id = track.videoId,
                title = track.title,
                artist = track.artists?.joinToString(", ") { it.name }.orEmpty(),
                album = track.album?.name.orEmpty(),
                duration = (track.durationSeconds?.toLong() ?: 0L) * 1000L,
                thumbnail = track.thumbnails?.lastOrNull()?.url.orEmpty(),
            )
        },
        title = this?.data?.playlistName.orEmpty(),
    )

internal fun roomPlaybackOrder(current: RoomTrack, queue: List<RoomTrack>): List<RoomTrack> =
    (listOf(current) + queue.filter { it.id != current.id })
        .filter { it.id.isNotBlank() }
        .distinctBy { it.id }

/** Room queues replace the local catalog; they cannot inherit its radio/playlist continuation. */
internal fun List<RoomTrack>.toGuestQueueData(title: String): QueueData.Data {
    val tracks = map { roomTrack ->
        Track(
            videoId = roomTrack.id, title = roomTrack.title,
            artists = roomTrack.artist.takeIf { it.isNotBlank() }?.let { listOf(Artist(null, it)) },
            album = roomTrack.album.takeIf { it.isNotBlank() }?.let { Album("", it) },
            thumbnails = roomTrack.thumbnail.takeIf { it.isNotBlank() }?.let { listOf(Thumbnail(0, it, 0)) },
            durationSeconds = (roomTrack.durationMs.coerceAtLeast(0L) / 1000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            duration = null, isAvailable = true, isExplicit = false, likeStatus = null,
            videoType = "MUSIC_VIDEO_TYPE_ATV", category = null, feedbackTokens = null, resultType = null,
        )
    }
    return QueueData.Data(
        listTracks = tracks, firstPlayedTrack = tracks.firstOrNull(),
        playlistName = title.takeIf { it.isNotBlank() }, playlistType = PlaylistType.PLAYLIST,
        playlistId = null, continuation = null,
    )
}

/** A delayed transport command belongs to the room and track that started its settle wait. */
internal fun mayPublishSettledPlayback(
    startedIn: ListenTogetherRoom,
    currentRoom: ListenTogetherRoom,
    expectedTrackId: String,
    activeTrackId: String?,
): Boolean =
    currentRoom.inRoom && currentRoom.isHost &&
        currentRoom.roomCode == startedIn.roomCode &&
        currentRoom.selfUserId == startedIn.selfUserId &&
        activeTrackId == expectedTrackId
