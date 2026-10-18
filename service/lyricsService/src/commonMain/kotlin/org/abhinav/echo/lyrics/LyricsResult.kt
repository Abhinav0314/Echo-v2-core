package org.abhinav.echo.lyrics

import kotlinx.coroutines.CancellationException

/** Cancelling a track lookup is lifecycle control, not a missing-provider response. */
internal inline fun <T> lyricsResult(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
