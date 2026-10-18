package org.abhinav.echo.listentogether

import io.ktor.client.plugins.websocket.webSocketSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Captures bytes from the production Ktor/OkHttp transport, without a public server or mock engine. */
class UserAgentHandshakeTest {
    @Test
    fun upgradeRequestContainsExactlyOneApplicationUserAgent() {
        val userAgent = "Echo/2.1.0 (club.vibescape.echo; Test OS 1)"
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            server.soTimeout = 10_000
            val client = echoRoomHttpClient(userAgent)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                scope.launch { runCatching { client.webSocketSession("ws://127.0.0.1:${server.localPort}/ws") } }
                server.accept().use { socket ->
                    socket.soTimeout = 10_000
                    val reader = socket.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                    val requestLine = reader.readLine()
                    val headers = mutableListOf<Pair<String, String>>()
                    while (true) {
                        val line = reader.readLine() ?: error("Connection closed before headers completed")
                        if (line.isEmpty()) break
                        headers += line.substringBefore(':') to line.substringAfter(':').trim()
                    }
                    assertEquals("GET /ws HTTP/1.1", requestLine)
                    assertTrue(
                        headers.any { (name, value) ->
                            name.equals("Upgrade", ignoreCase = true) && value.equals("websocket", ignoreCase = true)
                        },
                    )
                    assertEquals(
                        listOf(userAgent),
                        headers.filter { it.first.equals("User-Agent", ignoreCase = true) }.map { it.second },
                    )
                }
            } finally {
                scope.cancel()
                client.close()
            }
        }
    }
}
