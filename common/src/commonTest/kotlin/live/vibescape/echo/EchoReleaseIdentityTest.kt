package live.vibescape.echo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EchoReleaseIdentityTest {
    @Test
    fun distributionsUseSeparateCatalogs() {
        assertEquals("echo-v2", EchoReleaseIdentity.channelFor("club.vibescape.echo"))
        assertEquals("echo-vivo", EchoReleaseIdentity.channelFor("com.jio.media.jiobeats"))
        assertTrue(EchoReleaseIdentity.isReleasePackage("com.jio.media.jiobeats"))
        assertFalse(EchoReleaseIdentity.isReleasePackage("com.spotify.music"))
    }

    @Test
    fun debugAndUnknownPackagesDoNotReceiveProductionUpdates() {
        assertNull(EchoReleaseIdentity.channelFor("club.vibescape.echo.dev"))
        assertNull(EchoReleaseIdentity.channelFor("com.jio.media.jiobeats", isDevBuild = true))
        assertNull(EchoReleaseIdentity.channelFor("com.spotify.music"))
    }
}
