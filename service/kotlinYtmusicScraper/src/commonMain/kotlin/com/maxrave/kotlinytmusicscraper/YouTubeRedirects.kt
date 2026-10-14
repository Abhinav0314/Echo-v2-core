package com.maxrave.kotlinytmusicscraper

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRedirect

/** API requests and downloads may redirect between HTTPS providers, never to cleartext. */
internal fun HttpClientConfig<*>.installSecureYouTubeRedirects() {
    install(HttpRedirect) {
        checkHttpMethod = false
        allowHttpsDowngrade = false
    }
}
