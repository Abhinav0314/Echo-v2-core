package com.abhinav.kotlinytmusicscraper.utils

/** Both supported session cookies authorize the same YouTube Music origin. */
internal fun buildYouTubeAuthorizationHeader(
    cookies: Map<String, String>,
    timestamp: Long,
): String? {
    val sessionCookie =
        cookies["SAPISID"]?.takeIf { it.isNotEmpty() }
            ?: cookies["__Secure-3PAPISID"]?.takeIf { it.isNotEmpty() }
            ?: return null
    val hash = sha1("$timestamp $sessionCookie https://music.youtube.com")
    return "SAPISIDHASH ${timestamp}_$hash"
}
