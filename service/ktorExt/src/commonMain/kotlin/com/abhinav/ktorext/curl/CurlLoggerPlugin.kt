package com.abhinav.ktorext.curl

import io.ktor.client.plugins.api.SendingRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import kotlin.coroutines.cancellation.CancellationException

class CurlLoggerConfig {
    var logger: (String) -> Unit = { println(it) }
}

/**
 * Retains the installed plugin name for callers, but logs only method and origin.
 * Headers, bodies, URL credentials, paths and query strings never enter a log sink.
 * Logging never reads or replays a streaming request body.
 */
val CurlLogger = createClientPlugin("CurlLogger", ::CurlLoggerConfig) {
    val log = pluginConfig.logger
    on(SendingRequest) { request, _ ->
        try { log(requestLogMessage(request)) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Diagnostics cannot interrupt a request. */ }
    }
}

internal fun requestLogMessage(request: HttpRequestBuilder): String =
    "HTTP ${request.method.value} ${request.url.protocol.name}://${request.url.host}"
