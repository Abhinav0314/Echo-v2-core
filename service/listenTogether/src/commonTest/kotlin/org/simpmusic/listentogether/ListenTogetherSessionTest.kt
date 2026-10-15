package org.simpmusic.listentogether

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListenTogetherSessionTest {
    private class Transport : ListenTogetherTransport {
        override val events = MutableSharedFlow<ListenTogetherEvent>()
        val sent = mutableListOf<Pair<String, Any?>>()
        var connections = 0
        var forgotten = false
        var holdChangeTrack: CompletableDeferred<Unit>? = null
        override fun connect() { connections++ }
        override fun disconnect() = Unit
        override fun release() = Unit
        override fun forgetRoomSession() { forgotten = true }
        override fun serverNow(): Long = 1000L
        override fun positionAt(position: Long, effectiveAtServerTime: Long?, isPlaying: Boolean) = position
        override suspend fun send(msgType: String, payload: Any?): Boolean {
            if ((payload as? PlaybackActionPayload)?.action == PlaybackActions.CHANGE_TRACK) holdChangeTrack?.await()
            sent += msgType to payload
            return true
        }
        suspend fun message(type: String, payload: Any?) = events.emit(ListenTogetherEvent.Message(type, payload))
    }

    @Test
    fun repeatedConnectDoesNotLeaveAConnectedRoomStuckConnecting() = runBlocking {
        val transport = Transport()
        val session = ListenTogetherSession(transport, Dispatchers.Unconfined)
        try {
            session.connect()
            transport.events.emit(ListenTogetherEvent.Connected("1", true, false))
            session.connect()
            assertEquals(1, transport.connections)
            assertTrue(session.state.value.isConnected)
        } finally { session.release() }
    }

    @Test
    fun resumeWaitsForConfirmationThenRestoresLiveRoomAndRole() = runBlocking {
        val transport = Transport()
        val session = ListenTogetherSession(transport, Dispatchers.Unconfined)
        try {
            session.connect()
            transport.events.emit(ListenTogetherEvent.Connected("1", true, true))
            assertFalse(session.state.value.isConnected)
            transport.message(MessageTypes.RECONNECTED, ReconnectedPayload(
                roomCode = "ROOM", userId = "guest", isHost = false,
                state = RoomState(currentTrack = TrackInfo(id = "new-track"), isPlaying = true,
                    position = 9000L, lastUpdate = 2000L, queue = listOf(TrackInfo(id = "next")),
                    users = listOf(UserInfo("guest", "Echo", false, true))),
            ))
            val state = session.state.value
            assertTrue(state.isConnected)
            assertEquals("ROOM", state.roomCode)
            assertFalse(state.isHost)
            assertEquals("new-track", state.currentTrack?.id)
            assertEquals(2000L, state.lastActionServerTime)
            assertEquals(listOf("next"), state.queue.map { it.id })
            assertTrue(state.members.single().isConnected)
        } finally { session.release() }
    }

    @Test
    fun expiredResumeReturnsToConnectedLobbyWithNoStaleRoom() = runBlocking {
        val transport = Transport()
        val session = ListenTogetherSession(transport, Dispatchers.Unconfined)
        try {
            session.connect()
            transport.message(MessageTypes.ROOM_CREATED, RoomCreatedPayload("OLD", "host", "private-token"))
            transport.events.emit(ListenTogetherEvent.Connected("1", true, true))
            transport.message(MessageTypes.ERROR, ErrorPayload("session_expired", "Session expired"))
            assertTrue(session.state.value.isConnected)
            assertFalse(session.state.value.inRoom)
            assertEquals("Your room session has expired. Join the room again.", session.state.value.error)
        } finally { session.release() }
    }

    @Test
    fun leavingForgetsResumeTokenAndEveryPreviousRoomField() = runBlocking {
        val transport = Transport()
        val session = ListenTogetherSession(transport, Dispatchers.Unconfined)
        try {
            session.connect()
            transport.events.emit(ListenTogetherEvent.Connected("1", true, false))
            transport.message(MessageTypes.ROOM_CREATED, RoomCreatedPayload("ROOM", "host", "private-token"))
            session.leaveRoom().join()
            assertTrue(transport.forgotten)
            assertTrue(session.state.value.isConnected)
            assertEquals(ListenTogetherState(connection = ConnectionState.Connected("1")), session.state.value)
        } finally { session.release() }
    }

    @Test
    fun playCannotOvertakeSuspendedTrackPublication() = runBlocking {
        val transport = Transport()
        val session = ListenTogetherSession(transport, Dispatchers.Unconfined)
        try {
            session.connect()
            transport.events.emit(ListenTogetherEvent.Connected("1", true, false))
            transport.message(MessageTypes.ROOM_CREATED, RoomCreatedPayload("ROOM", "host", "private-token"))
            val gate = CompletableDeferred<Unit>()
            transport.holdChangeTrack = gate
            val change = session.sendPlaybackAction(PlaybackActions.CHANGE_TRACK, "song", 0L, TrackInfo(id = "song"))
            val play = session.sendPlaybackAction(PlaybackActions.PLAY, "song", 5000L, null)
            yield()
            assertTrue(transport.sent.isEmpty())
            gate.complete(Unit)
            change.join()
            play.join()
            assertEquals(listOf(PlaybackActions.CHANGE_TRACK, PlaybackActions.PLAY), transport.sent.map { (it.second as PlaybackActionPayload).action })
        } finally { session.release() }
    }

    @Test
    fun hostRejectionKeepsLobbyConnectedAndExplainsServerApproval() = runBlocking {
        val transport = Transport()
        val session = ListenTogetherSession(transport, Dispatchers.Unconfined)
        try {
            session.connect()
            transport.events.emit(ListenTogetherEvent.Connected("1", true, false))
            transport.message(MessageTypes.ERROR, ErrorPayload("host_not_allowed", "Only allowlisted clients can host rooms"))
            assertTrue(session.state.value.isConnected)
            assertFalse(session.state.value.inRoom)
            assertTrue(session.state.value.error.orEmpty().contains("Echo-approved server"))
        } finally { session.release() }
    }

    @Test
    fun serverErrorMessagesAndUnknownCodesNeverReachUserState() = runBlocking {
        val transport=Transport()
        val session=ListenTogetherSession(transport,Dispatchers.Unconfined)
        try {
            session.connect()
            transport.events.emit(ListenTogetherEvent.Connected("1",true,false))
            transport.message(MessageTypes.ERROR,ErrorPayload("private@example.com access_token=secret","internal server path https://private.invalid"))
            assertEquals("Listen Together couldn't finish that request. Please try again.",session.state.value.error)
            assertTrue(session.state.value.isConnected)
            transport.message(MessageTypes.ERROR,ErrorPayload("room_not_found","private room owner secret"))
            assertEquals("This room couldn't be found. Check the room code or ask the host for a new one.",session.state.value.error)
        } finally { session.release() }
    }

    @Test
    fun joinRejectionAndKickDiscardRemoteReasonText() = runBlocking {
        val transport=Transport()
        val session=ListenTogetherSession(transport,Dispatchers.Unconfined)
        try {
            session.connect()
            transport.events.emit(ListenTogetherEvent.Connected("1",true,false))
            transport.message(MessageTypes.JOIN_REJECTED,JoinRejectedPayload("private@example.com internal-error"))
            assertEquals("The host declined your request to join this room.",session.state.value.error)
            transport.message(MessageTypes.KICKED,KickedPayload("private@example.com internal-error"))
            assertEquals("You were removed from the room. Ask the host before joining again.",session.state.value.error)
            assertFalse(session.state.value.inRoom)
        } finally { session.release() }
    }
}
