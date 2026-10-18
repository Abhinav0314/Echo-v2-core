package com.abhinav.domain.extension

/** Older Echo queues stored this provider's HTTP fallback. Preserve their artwork over TLS. */
fun String.secureYouTubeArtworkUrl(): String =
    if (startsWith("http://i.ytimg.com/", ignoreCase = true)) "https://${substring(7)}" else this
