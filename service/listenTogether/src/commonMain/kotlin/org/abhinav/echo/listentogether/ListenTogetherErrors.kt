package org.abhinav.echo.listentogether

/** Server codes select owned copy; neither remote messages nor unknown codes reach UI/logs. */
internal fun listenTogetherErrorMessage(code: String): String = when(code) {
    "auth_required" -> "Sign in to your Echo account to listen together."
    "password_required" -> "This room has a password. Enter it below to join."
    "password_incorrect" -> "Check the room password and try again."
    "invalid_password" -> "Choose a longer room password and try again."
    "not_host" -> "Only the room host can remove people or change who can join."
    "not_in_room" -> "You are no longer connected to this room. Join it again and try again."
    "user_not_found" -> "This person has already left or is reconnecting. Try again when they are connected."
    "cannot_kick_self" -> "Use Leave room if you want to leave."
    "cannot_transfer_to_self" -> "You are already the room host. Choose another listener."
    "missing_user_id", "invalid_payload" -> "That room action could not be sent correctly. Reopen the room and try again."
    "join_rejected" -> "You cannot join this room. Ask the host for help."
    "room_full" -> "This room is full. Try again after someone leaves."
    "room_limit_reached" -> "You already host the maximum number of rooms. Close one first."
    "host_not_allowed" -> "This server does not allow Echo to host rooms. Choose an Echo-approved server in Listen Together settings. You can still join rooms when the server permits it."
    "session_not_found", "session_expired" -> "Your room session has expired. Join the room again."
    "room_not_found" -> "This room couldn't be found. Check the room code or ask the host for a new one."
    "unsupported_client" -> "This server doesn't support this version of Echo. Check for an app update or choose another server."
    "rate_limited" -> "Too many requests. Wait a moment and try again."
    else -> "Listen Together couldn't finish that request. Please try again."
}

/** Only local, owned connection guidance may reach the screen. */
internal fun listenTogetherConnectionErrorMessage(reason: String?): String = when (reason) {
    "Sign in to your Echo account to listen together.",
    "Use the official Echo server to listen together." -> reason
    else -> "Couldn't connect to Echo Sync. Check your connection and try again."
}
