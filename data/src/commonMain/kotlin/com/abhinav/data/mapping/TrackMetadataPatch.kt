package com.abhinav.data.mapping

import com.abhinav.domain.data.model.browse.album.Track
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal data class TrackMetadataPatch(
    val artistIdsJson: String?, val artistNamesJson: String?, val albumId: String?,
    val albumName: String?, val durationSeconds: Int?, val duration: String?,
) {
    val isEmpty: Boolean get() = artistIdsJson == null && albumId == null && durationSeconds == null
}

/** Domain Track lost endpoint types. Only channel IDs can verify artist entries at this layer. */
internal fun Track.metadataPatch(): TrackMetadataPatch {
    val verifiedArtists = artists.orEmpty().filter {
        it.id?.matches(channelIdPattern) == true && it.name.isNotBlank()
    }.distinctBy { it.id }
    val verifiedAlbum = album?.takeIf { it.id.startsWith("MPRE") && it.name.isNotBlank() }
    val seconds = durationSeconds?.takeIf { it > 0 }
    return TrackMetadataPatch(
        artistIdsJson = verifiedArtists.takeIf { it.isNotEmpty() }?.let { Json.encodeToString(it.map { artist -> artist.id!! }) },
        artistNamesJson = verifiedArtists.takeIf { it.isNotEmpty() }?.let { Json.encodeToString(it.map { artist -> artist.name }) },
        albumId = verifiedAlbum?.id,
        albumName = verifiedAlbum?.name,
        durationSeconds = seconds,
        duration = seconds?.let {
            if (it >= 3600) "${it / 3600}:${((it / 60) % 60).toString().padStart(2, '0')}:${(it % 60).toString().padStart(2, '0')}"
            else "${it / 60}:${(it % 60).toString().padStart(2, '0')}"
        },
    )
}

private val channelIdPattern = Regex("UC[A-Za-z0-9_-]{22}")
