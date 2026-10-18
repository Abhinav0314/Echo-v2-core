package com.abhinav.data.repository

import com.abhinav.data.mapping.toLyrics
import com.abhinav.domain.data.model.metadata.Lyrics
import com.abhinav.domain.utils.Resource
import com.abhinav.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** The LRCLIB repository path always answers, including a successful search with no usable match. */
internal fun lrclibLyricsFlow(
    request: suspend () -> Result<org.abhinav.echo.lyrics.domain.Lyrics?>,
): Flow<Resource<Lyrics>> = flow {
    val result = request()
    result.exceptionOrNull()?.let { failure ->
        if (failure is CancellationException) throw failure
        Logger.w("EchoLyrics", "LRCLIB request failed (${failure::class.simpleName}); trying saved lyrics")
    }
    val lyrics = result.getOrNull()?.toLyrics()?.takeIf { value ->
        !value.error && value.lines?.any { it.words.isNotBlank() } == true
    }
    // An empty flow left the player waiting forever and skipped its saved/offline fallback.
    if (lyrics == null && result.isSuccess) {
        Logger.d("EchoLyrics", "LRCLIB found no usable lyrics; trying saved lyrics")
    }
    emit(if (lyrics != null) Resource.Success(lyrics) else Resource.Error("No lyrics found"))
}
