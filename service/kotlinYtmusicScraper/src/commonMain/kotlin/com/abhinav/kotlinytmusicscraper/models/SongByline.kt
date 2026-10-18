package com.abhinav.kotlinytmusicscraper.models

import com.abhinav.kotlinytmusicscraper.utils.parseTime

/** Search layouts move subtitle groups; navigation endpoint types identify each field. */
data class SongByline(val artists: List<Artist>, val album: Album?, val duration: Int?)

fun List<Run>.songByline(): SongByline {
    val artists = mapNotNull { run ->
        val endpoint = run.navigationEndpoint?.browseEndpoint ?: return@mapNotNull null
        val type = endpoint.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
        val artist = type in artistPageTypes || (type == null && channelIdPattern.matches(endpoint.browseId))
        if (artist && endpoint.browseId.isNotBlank() && run.text.isNotBlank()) Artist(run.text, endpoint.browseId) else null
    }.distinctBy { it.id }
    val album = firstNotNullOfOrNull { run ->
        val endpoint = run.navigationEndpoint?.browseEndpoint ?: return@firstNotNullOfOrNull null
        val type = endpoint.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
        val isAlbum = type in albumPageTypes || (type == null && endpoint.browseId.startsWith("MPRE"))
        if (isAlbum && endpoint.browseId.isNotBlank() && run.text.isNotBlank()) Album(run.text, endpoint.browseId) else null
    }
    val duration = asReversed().firstNotNullOfOrNull { run ->
        val text = run.text.trim()
        // Linked numeric artist/album names and view counters cannot become a duration.
        if (run.navigationEndpoint == null && durationPattern.matches(text)) text.parseTime()?.takeIf { it > 0 } else null
    }
    return SongByline(artists, album, duration)
}

fun MusicResponsiveListItemRenderer.songByline(): SongByline =
    (flexColumns.drop(1) + fixedColumns.orEmpty()).flatMap {
        it.musicResponsiveListItemFlexColumnRenderer.text?.runs.orEmpty()
    }.songByline()

private val artistPageTypes = setOf("MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL")
private val albumPageTypes = setOf("MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_AUDIOBOOK")
private val channelIdPattern = Regex("UC[A-Za-z0-9_-]{22}")
private val durationPattern = Regex("[0-9]{1,3}[:.][0-5][0-9](?:[:.][0-5][0-9])?")
