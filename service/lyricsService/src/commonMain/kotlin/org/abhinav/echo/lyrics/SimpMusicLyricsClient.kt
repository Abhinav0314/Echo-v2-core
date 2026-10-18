package org.abhinav.echo.lyrics

import com.abhinav.ktorext.crypto.Hmac
import com.abhinav.ktorext.crypto.HmacUri
import com.abhinav.logger.Logger
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import org.abhinav.echo.lyrics.am.AMAlbumResource
import org.abhinav.echo.lyrics.am.AMSongWithAlbum
import org.abhinav.echo.lyrics.am.parseAMHlsVariants
import org.abhinav.echo.lyrics.am.pickAMRendition
import org.abhinav.echo.lyrics.am.AMArtistResource
import org.abhinav.echo.lyrics.am.AMSearchResponse
import org.abhinav.echo.lyrics.models.request.LyricsBody
import org.abhinav.echo.lyrics.models.request.TranslatedLyricsBody
import org.abhinav.echo.lyrics.models.response.BetterLyricsResponse
import org.abhinav.echo.lyrics.models.response.LrclibObject
import org.abhinav.echo.lyrics.models.response.LyricsResponse
import org.abhinav.echo.lyrics.models.response.TranslatedLyricsResponse
import org.abhinav.echo.lyrics.parser.parseSyncedLyrics
import org.abhinav.echo.lyrics.parser.parseUnsyncedLyrics
import kotlin.math.abs

private const val TAG = "SimpMusicLyricsClient"

/** An error the SimpMusic Lyrics API answered with; [code] 404 means nobody has added that song yet. */
class SimpMusicLyricsApiException(
    val code: Int,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class LyricsProviderDisabledException : IllegalStateException("This lyrics provider is currently unavailable.")

class SimpMusicLyricsClient(val communityApiEnabled: Boolean = true) {
    private val algorithm = ""

    private val hmacService = Hmac("HmacSHA256", "simpmusic-lyrics")
    private val lyricsService by lazy { SimpMusicLyrics() }
    private fun requireCommunityApi() {
        if (!communityApiEnabled) throw LyricsProviderDisabledException()
    }
    private var insertingLyrics: Pair<String?, Boolean> = (null to false)
    private val isInsertingLyrics: Boolean
        get() = insertingLyrics.second

    private var tooManyRequest: Boolean = false

    private var insertingTranslatedLyrics: Pair<String?, Boolean> = (null to false)
    private val isInsertingTranslatedLyrics: Boolean
        get() = insertingTranslatedLyrics.second

    suspend fun getLyrics(videoId: String): Result<List<LyricsResponse>> =
        lyricsResult {
            requireCommunityApi()
            lyricsService.findLyricsByVideoId(videoId).bodyOrThrow<List<LyricsResponse>>()
        }

    suspend fun getTranslatedLyrics(
        videoId: String,
        language: String,
    ): Result<TranslatedLyricsResponse> =
        lyricsResult {
            requireCommunityApi()
            if (language.length != 2) {
                throw IllegalArgumentException("Language code must be a 2-letter code")
            }
            lyricsService.findTranslatedLyrics(videoId, language).bodyOrThrow<TranslatedLyricsResponse>()
        }

    suspend fun insertLyrics(lyricsBody: LyricsBody): Result<LyricsResponse> =
        lyricsResult {
            requireCommunityApi()
            if (tooManyRequest) {
                throw IllegalStateException("Too many requests, please wait before trying again.")
            }
            if (isInsertingLyrics && insertingLyrics.first == lyricsBody.videoId) {
                throw IllegalStateException("Already inserting lyrics, please wait until the current operation is complete.")
            }
            insertingLyrics = lyricsBody.videoId to true
            val hmacTimestamp =
                hmacService.getMacTimestampPair(
                    HmacUri.BASE_HMAC_URI,
                )
            lyricsService.insertLyrics(lyricsBody, hmacTimestamp).bodyOrThrow<LyricsResponse>()
        }

    suspend fun insertTranslatedLyrics(translatedLyricsBody: TranslatedLyricsBody): Result<TranslatedLyricsResponse> =
        lyricsResult {
            requireCommunityApi()
            if (translatedLyricsBody.language.length != 2) {
                throw IllegalArgumentException("Language code must be a 2-letter code")
            }
            if (isInsertingTranslatedLyrics && insertingTranslatedLyrics.first == translatedLyricsBody.videoId) {
                throw IllegalStateException("Already inserting translated lyrics, please wait until the current operation is complete.")
            }
            insertingTranslatedLyrics = translatedLyricsBody.videoId to true
            val hmacTimestamp =
                hmacService.getMacTimestampPair(
                    HmacUri.TRANSLATED_HMAC_URI,
                )
            lyricsService.insertTranslatedLyrics(translatedLyricsBody, hmacTimestamp).bodyOrThrow<TranslatedLyricsResponse>()
        }

    suspend fun voteLyrics(
        lyricsId: String,
        upvote: Boolean,
    ): Result<LyricsResponse> =
        lyricsResult {
            requireCommunityApi()
            val hmacTimestamp =
                hmacService.getMacTimestampPair(
                    HmacUri.VOTE_HMAC_URI,
                )
            lyricsService.voteLyrics(lyricsId, upvote, hmacTimestamp).bodyOrThrow<LyricsResponse>()
        }

    suspend fun voteTranslatedLyrics(
        translatedLyricsId: String,
        upvote: Boolean,
    ): Result<TranslatedLyricsResponse> =
        lyricsResult {
            requireCommunityApi()
            val hmacTimestamp =
                hmacService.getMacTimestampPair(
                    HmacUri.VOTE_TRANSLATED_HMAC_URI,
                )
            lyricsService.voteTranslatedLyrics(translatedLyricsId, upvote, hmacTimestamp).bodyOrThrow<TranslatedLyricsResponse>()
        }

    suspend fun searchLrclibLyrics(
        q_track: String,
        q_artist: String,
        duration: Int?,
    ) = lyricsResult {
        val rs =
            lyricsService
                .searchLrclibLyrics(
                    q_track = q_track,
                    q_artist = q_artist,
                ).body<List<LrclibObject>>()
        val lrclibObject: LrclibObject? =
            if (duration != null) {
                rs.find { abs(it.duration.toInt() - duration) <= 10 }
            } else {
                rs.firstOrNull()
            }
        if (lrclibObject != null) {
            val syncedLyrics = lrclibObject.syncedLyrics
            val plainLyrics = lrclibObject.plainLyrics
            if (!syncedLyrics.isNullOrEmpty()) {
                parseSyncedLyrics(syncedLyrics)
            } else if (!plainLyrics.isNullOrEmpty()) {
                parseUnsyncedLyrics(plainLyrics)
            } else {
                null
            }
        } else {
            null
        }
    }

    suspend fun searchBetterLyrics(
        q_track: String,
        q_artist: String,
        durationSeconds: Int?,
    ) = lyricsResult {
        val rs =
            lyricsService
                .searchBetterLyrics(
                    q_track = q_track,
                    q_artist = q_artist,
                    durationSeconds = durationSeconds,
                ).body<BetterLyricsResponse>()
        rs.ttml
    }

    suspend fun searchAMArtist(
        name: String,
        limit: Int = 5,
    ): Result<List<AMArtistResource>> =
        lyricsResult {
            val response = lyricsService.searchAMArtist(name, limit)
            if (response.status.value !in 200..299) {
                throw Exception("AM search failed: ${response.status.value}")
            }
            val parsed = response.body<AMSearchResponse>()
            val resources = parsed.resources?.artists.orEmpty()
            // Keep the search ranking from results.data; fall back to the resources map order.
            parsed.results
                ?.artists
                ?.data
                ?.mapNotNull { resources[it.id] }
                ?: resources.values.toList()
        }

    /**
     * Fetch a single artist by id, including [AMEditorialArtwork] (name-logo PNG) and keyColor.
     * Returns null when the id is not present in the response.
     */
    suspend fun getAMArtist(id: String): Result<AMArtistResource?> =
        lyricsResult {
            val response = lyricsService.getAMArtist(id)
            if (response.status.value !in 200..299) {
                throw Exception("AM artist fetch failed: ${response.status.value}")
            }
            response
                .body<AMSearchResponse>()
                .resources
                ?.artists
                ?.get(id)
        }

    /**
     * Search the AM catalog for albums, ranked the way AM ranked them. Each result already carries
     * its animated artwork when it has one, so the caller needs no follow-up request.
     */
    suspend fun searchAMAlbum(
        query: String,
        limit: Int = 5,
    ): Result<List<AMAlbumResource>> =
        lyricsResult {
            val response = lyricsService.searchAMAlbum(query, limit)
            if (response.status.value !in 200..299) {
                throw Exception("AM album search failed: ${response.status.value}")
            }
            val parsed = response.body<AMSearchResponse>()
            val resources = parsed.resources?.albums.orEmpty()
            // Keep the search ranking from results.data; fall back to the resources map order.
            parsed.results
                ?.albums
                ?.data
                ?.mapNotNull { resources[it.id] }
                ?: resources.values.toList()
        }

    /**
     * Tracks reached by title, each paired with the album it appears on.
     *
     * The pairing comes from `relationships.albums`, never from the flat `resources.albums` map:
     * that map holds every album any hit belongs to, so reading an album out of it directly
     * attaches whichever release happens to carry artwork to whatever track was searched for.
     * Order follows the search ranking; the caller still decides which hit is the right track.
     */
    suspend fun searchAMSongsWithAlbums(
        query: String,
        limit: Int = 5,
    ): Result<List<AMSongWithAlbum>> =
        lyricsResult {
            val response = lyricsService.searchAMSongs(query, limit)
            if (response.status.value !in 200..299) {
                throw Exception("AM song search failed: ${response.status.value}")
            }
            val parsed = response.body<AMSearchResponse>()
            val songs = parsed.resources?.songs.orEmpty()
            val albums = parsed.resources?.albums.orEmpty()
            val ranked =
                parsed.results
                    ?.songs
                    ?.data
                    ?.mapNotNull { songs[it.id] }
                    ?: songs.values.toList()
            ranked.mapNotNull { song ->
                val albumId =
                    song.relationships
                        ?.albums
                        ?.data
                        ?.firstOrNull()
                        ?.id
                val album = albumId?.let { albums[it] } ?: return@mapNotNull null
                AMSongWithAlbum(
                    songName = song.attributes?.name,
                    artistName = song.attributes?.artistName,
                    durationInMillis = song.attributes?.durationInMillis,
                    album = album,
                )
            }
        }

    /**
     * Resolve an animated-artwork master playlist down to the single rendition worth playing, and
     * return its media-playlist url.
     *
     * Doing this here rather than leaving it to the player is what makes the two platforms agree:
     * mpv defaults to `--hls-bitrate=max` and would take the top of the ladder — on a measured
     * artwork that is 2048x2732 10-bit HEVC at 20 Mbps, 52 MB for a 22-second loop — while
     * ExoPlayer would pick by its own bandwidth estimate. Handing both a chosen rendition removes
     * the question. Returns null when the url is not a master playlist.
     */
    suspend fun selectAMRendition(
        masterUrl: String,
        minWidth: Int,
    ): Result<String?> =
        lyricsResult {
            val response = lyricsService.fetchAMHlsPlaylist(masterUrl)
            if (response.status.value !in 200..299) {
                throw Exception("AM playlist fetch failed: ${response.status.value}")
            }
            parseAMHlsVariants(response.bodyAsText(), masterUrl)
                .pickAMRendition(minWidth)
                ?.uri
        }

    private suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
        if (this.status.value == 429) {
            tooManyRequest = true
            Logger.e(TAG, "Too many requests: ${this.status.value}")
        } else {
            tooManyRequest = false
        }
        return lyricsBodyOrThrow()
    }
}
