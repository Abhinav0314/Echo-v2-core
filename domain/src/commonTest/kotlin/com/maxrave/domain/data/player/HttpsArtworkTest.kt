package com.maxrave.domain.data.player

import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.videos.VideosResult
import com.maxrave.domain.extension.toGenericMediaItem
import com.maxrave.domain.extension.secureYouTubeArtworkUrl
import com.maxrave.domain.utils.toTrack
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpsArtworkTest {
    @Test
    fun onlyTheKnownLegacyProviderIsUpgraded() {
        assertEquals("https://i.ytimg.com/vi/video-id/maxresdefault.jpg", "http://i.ytimg.com/vi/video-id/maxresdefault.jpg".secureYouTubeArtworkUrl())
        assertEquals("http://i.ytimg.com.evil.test/art", "http://i.ytimg.com.evil.test/art".secureYouTubeArtworkUrl())
        assertEquals("http://my-server.test/art", "http://my-server.test/art".secureYouTubeArtworkUrl())
        assertEquals("https://i.ytimg.com/art", "https://i.ytimg.com/art".secureYouTubeArtworkUrl())
    }
    @Test
    fun missingSearchVideoArtworkRemainsLoadableUnderTheHttpsOnlyPolicy() {
        val result = VideosResult(null, null, null, null, null, null, "Song", "video-id", null, null, "")
        val track = result.toTrack()
        assertEquals("https://i.ytimg.com/vi/video-id/maxresdefault.jpg", track.thumbnails?.single()?.url)
        assertEquals("https://i.ytimg.com/vi/video-id/maxresdefault.jpg", track.toGenericMediaItem().metadata.artworkUri)
    }

    @Test
    fun aTrackWithoutArtworkGetsASecurePlaybackFallback() {
        val track = Track(null, null, null, null, true, false, null, null, "Song", "video-id", null, null, null, null)
        assertEquals("https://i.ytimg.com/vi/video-id/maxresdefault.jpg", track.toGenericMediaItem().metadata.artworkUri)
    }
}
