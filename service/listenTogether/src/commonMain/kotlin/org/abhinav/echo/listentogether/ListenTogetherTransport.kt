package org.abhinav.echo.listentogether

import kotlinx.coroutines.flow.SharedFlow

/** Protocol transport boundary, also used by Android host tests without a public server. */
interface ListenTogetherTransport {
    val events: SharedFlow<ListenTogetherEvent>
    fun connect()
    fun disconnect()
    fun release()
    fun forgetRoomSession()
    fun serverNow(): Long?
    fun positionAt(position: Long, effectiveAtServerTime: Long?, isPlaying: Boolean): Long
    suspend fun send(msgType: String, payload: Any? = null): Boolean
}
