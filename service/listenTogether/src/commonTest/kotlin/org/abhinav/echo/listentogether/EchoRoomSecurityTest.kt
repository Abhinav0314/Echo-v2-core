package org.abhinav.echo.listentogether

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EchoRoomSecurityTest {
    @Test fun onlyTheExactOfficialEndpointCanRequestCredentials() = runBlocking {
        for (url in listOf("ws://listen.echo.vibescape.club/ws", "wss://listen.echo.vibescape.club.evil.invalid/ws", "wss://listen.echo.vibescape.club@evil.invalid/ws", "wss://listen.echo.vibescape.club/ws?token=anything", "wss://metroserverx.meowery.eu/ws", "wss://listen.echo.vibescape.club:443/ws")) {
            assertFalse(isTrustedEchoRoomServer(url))
            var requests = 0
            val client = ListenTogetherClient("test", "Echo/test", serverUrl = { url }, accessToken = { requests++; "must-not-leave-device" }, dispatcher = Dispatchers.Unconfined)
            val disconnected = CompletableDeferred<ListenTogetherEvent.Disconnected>()
            val collector = launch(start = CoroutineStart.UNDISPATCHED) { client.events.collect { if (it is ListenTogetherEvent.Disconnected) disconnected.complete(it) } }
            try {
                client.connect()
                assertEquals(0, requests)
                assertFalse(withTimeout(1000) { disconnected.await() }.willRetry)
            } finally { collector.cancel(); client.release() }
        }
        assertTrue(isTrustedEchoRoomServer(ListenTogetherClient.DEFAULT_SERVER_URL))
    }

    @Test fun signedOutUsersCannotOpenAnOfficialConnection() = runBlocking {
        var requests = 0
        val client = ListenTogetherClient("test", "Echo/test", accessToken = { requests++; null }, dispatcher = Dispatchers.Unconfined)
        val disconnected = CompletableDeferred<ListenTogetherEvent.Disconnected>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) { client.events.collect { if (it is ListenTogetherEvent.Disconnected) disconnected.complete(it) } }
        try {
            client.connect()
            assertEquals(1, requests)
            val event = withTimeout(1000) { disconnected.await() }
            assertTrue(event.reason?.contains("Sign in") == true)
            assertFalse(event.willRetry)
        } finally { collector.cancel(); client.release() }
    }

    @Test fun passwordsUseTheAppendedWireFields() {
        val codec = MessageCodec(false)
        val (_, create) = codec.decode(codec.encode(MessageTypes.CREATE_ROOM, CreateRoomPayload("A", "secret12", true)))
        assertEquals("0a0141120873656372657431321801", create.joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') })
        val (_, join) = codec.decode(codec.encode(MessageTypes.JOIN_ROOM, JoinRoomPayload("ABCD1234", "A", "secret12")))
        assertEquals("0a0841424344313233341201411a087365637265743132", join.joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') })
    }
}
