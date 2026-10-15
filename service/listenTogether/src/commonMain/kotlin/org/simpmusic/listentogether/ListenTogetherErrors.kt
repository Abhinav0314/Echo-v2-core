package org.simpmusic.listentogether

/** Server codes select owned copy; neither remote messages nor unknown codes reach UI/logs. */
internal fun listenTogetherErrorMessage(code: String): String = when(code) {
    "host_not_allowed" -> "This server does not allow Echo to host rooms. Choose an Echo-approved server in Listen Together settings. You can still join rooms when the server permits it."
    "session_not_found", "session_expired" -> "Your room session has expired. Join the room again."
    "room_not_found" -> "This room couldn't be found. Check the room code or ask the host for a new one."
    "unsupported_client" -> "This server doesn't support this version of Echo. Check for an app update or choose another server."
    "rate_limited" -> "Too many requests. Wait a moment and try again."
    else -> "Listen Together couldn't finish that request. Please try again."
}
