package org.abhinav.echo.lyrics

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout

/** Shared by every lyric read; the full request deadline includes connection and body reads. */
internal fun HttpClientConfig<*>.installLyricsTimeouts(requestDeadlineMillis: Long = 12_000L) {
    install(HttpTimeout) {
        connectTimeoutMillis = requestDeadlineMillis.coerceAtMost(5_000L)
        socketTimeoutMillis = requestDeadlineMillis.coerceAtMost(8_000L)
        requestTimeoutMillis = requestDeadlineMillis
    }
}
