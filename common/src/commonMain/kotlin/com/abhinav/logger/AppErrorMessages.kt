package com.abhinav.logger

/** Fixed, shareable wording. Raw exception messages, URLs and server bodies are never returned. */
object AppErrorMessages {
    fun area(context: String): String {
        val key = context.lowercase()
        return when {
            "lyric" in key || "caption" in key -> "Lyrics"
            "canvas" in key || "artwork" in key || "thumbnail" in key || "image" in key -> "Artwork"
            "backup" in key || "restore" in key -> "Backup and restore"
            "download" in key -> "Downloads"
            "update" in key && "format" !in key || "installer" in key -> "App updates"
            "search" in key -> "Search"
            "listentogether" in key || "room" in key -> "Listen Together"
            "sync" in key -> "Library sync"
            "auth" in key || "signin" in key || "login" in key || "account" in key || "credential" in key -> "Sign-in and account"
            "profile" in key || "avatar" in key || "email" in key || "password" in key -> "Account details"
            "playlist" in key -> "Playlists"
            "queue" in key -> "Play queue"
            "player" in key || "playback" in key || "stream" in key || "media" in key || "format" in key -> "Music playback"
            "album" in key -> "Albums"
            "artist" in key -> "Artists"
            "home" in key || "chart" in key || "browse" in key || "discover" in key -> "Music discovery"
            "database" in key || "datasource" in key || "storage" in key || "cache" in key -> "Saved data"
            "notification" in key -> "Notifications"
            "setting" in key || "theme" in key || "remoteconfig" in key -> "Settings"
            "cast" in key || "bluetooth" in key || "output" in key -> "Audio connection"
            "share" in key || "link" in key -> "Shared links"
            "spotify" in key || "lastfm" in key || "discord" in key || "tidal" in key -> "Connected services"
            "library" in key || "song" in key -> "Your library"
            else -> "Echo"
        }
    }

    fun reason(failure: Throwable?, httpStatus: Int? = null): String {
        val names = generateSequence(failure) { it.cause }.take(6).map { it::class.simpleName.orEmpty() }.toList()
        return when {
            names.any { it.contains("Timeout") } -> "The connection timed out. Try again."
            names.any { it.contains("Cancellation") } -> "The operation was stopped before it finished."
            httpStatus == 401 -> "Your sign-in has expired. Sign in again."
            httpStatus == 403 -> "The service did not allow this request."
            httpStatus == 404 || "NoSuchElementException" in names -> "The requested content was not found."
            httpStatus == 409 -> "This changed elsewhere. Refresh and try again."
            httpStatus == 429 -> "The service is receiving too many requests. Try again later."
            httpStatus != null && httpStatus in 500..599 || "ServerResponseException" in names -> "The service is temporarily unavailable. Try again later."
            names.any { it.contains("SSL") || it.contains("Certificate") } -> "A secure connection could not be established. Check your connection and phone date."
            names.any { it.contains("Serialization") || it.contains("Decoding") || it.contains("Parsing") || it.contains("Json") } -> "The service returned data Echo could not read. Try again later."
            names.any { it in setOf("UnknownHostException", "UnresolvedAddressException", "ConnectException", "SocketException") } -> "Could not connect to the service. Check your internet connection."
            "SQLiteFullException" in names -> "Your device storage is full. Free some space and try again."
            names.any { it.startsWith("SQLite") } -> "Saved data could not be read or updated. Try restarting Echo."
            "FileNotFoundException" in names -> "The file is missing or no longer accessible."
            "SecurityException" in names -> "A required permission or access is unavailable. Check the app permissions."
            "IOException" in names -> "The operation was interrupted while reading or transferring data. Try again."
            httpStatus != null && httpStatus in 400..499 || "ClientRequestException" in names -> "The service could not accept this request."
            else -> "This operation could not be completed. Try again; if it keeps happening, contact Echo support."
        }
    }

    fun format(context: String, failure: Throwable? = null, httpStatus: Int? = null): String =
        area(context) + ": " + reason(failure, httpStatus)

    /** Includes only class/method identifiers from a local caller frame, never exception headers. */
    internal fun callerContext(trace: String): String = trace.lineSequence().map { it.trim() }
        .firstOrNull { it.startsWith("at ") && !it.contains("com.abhinav.logger.") }
        ?.substringBefore('(').orEmpty()
}
