package org.abhinav.echo.lyrics

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import org.abhinav.echo.lyrics.models.response.BaseResponse

/** Check the transport before decoding: blocked requests often contain an HTML challenge. */
internal suspend inline fun <reified T> HttpResponse.lyricsBodyOrThrow(): T {
    val statusCode = status.value
    val mediaType = headers[HttpHeaders.ContentType]?.substringBefore(';')?.trim()?.lowercase()
    if (statusCode !in 200..299) {
        throw SimpMusicLyricsApiException(statusCode, "Lyrics provider returned HTTP $statusCode (${mediaType ?: "unknown content type"})")
    }
    if (mediaType != "application/json" && mediaType?.endsWith("+json") != true) {
        throw SimpMusicLyricsApiException(statusCode, "Lyrics provider returned an unexpected content type (${mediaType ?: "missing"})")
    }
    val response = try {
        body<BaseResponse<T>>()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        throw SimpMusicLyricsApiException(statusCode, "Lyrics provider JSON could not be decoded (${failure::class.simpleName})", failure)
    }
    response.error?.let { error ->
        throw SimpMusicLyricsApiException(error.code, "Lyrics provider rejected the request (code ${error.code})")
    }
    if (!response.success) {
        throw SimpMusicLyricsApiException(statusCode, "Lyrics provider reported an unsuccessful response")
    }
    return response.data ?: throw SimpMusicLyricsApiException(statusCode, "Lyrics provider returned no data")
}
