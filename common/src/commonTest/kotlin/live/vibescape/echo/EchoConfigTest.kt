package live.vibescape.echo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EchoConfigTest {
    @Test fun versionTwoIdentityAndCatalogStaySeparateFromVersionOne() {
        assertEquals("club.vibescape.echo", EchoConfig.APPLICATION_ID)
        assertEquals("club.vibescape.echo.dev", EchoConfig.DEBUG_APPLICATION_ID)
        assertEquals("echo_v2_app_versions", EchoConfig.RELEASE_CATALOG_TABLE)
        assertEquals("echo-v2", EchoConfig.RELEASE_CHANNEL)
        assertEquals("https://echo.vibescape.club/echo-v2.apk", EchoConfig.APK_URL)
    }

    @Test fun appHostUsesOnlyTheCanonicalClubDomain() {
        assertTrue(EchoConfig.isAppHost("ECHO.VIBESCAPE.CLUB"))
        assertFalse(EchoConfig.isAppHost("echo.vibescape.live"))
        assertFalse(EchoConfig.isAppHost("echo.vibescape.club.example.org"))
        assertFalse(EchoConfig.isAppHost(null))
    }
    @Test fun apkDownloadRequiresTrustedHttpsAuthority() {
        assertTrue(isTrustedEchoApkUrl("https://echo.vibescape.club/echo.apk"))
        assertTrue(isTrustedEchoApkUrl("https://github.com/Abhinav0314/Echo-App/releases/download/echo-v2.0.0/echo.apk"))
        assertTrue(isTrustedEchoApkUrl("https://release-assets.githubusercontent.com/asset?download=opaque"))
        assertFalse(isTrustedEchoApkUrl("http://echo.vibescape.club/echo.apk"))
        assertFalse(isTrustedEchoApkUrl("https://echo.vibescape.club.attacker.org/echo.apk"))
        assertFalse(isTrustedEchoApkUrl("https://user@echo.vibescape.club/echo.apk"))
        assertFalse(isTrustedEchoApkUrl("https://echo.vibescape.club:8080/echo.apk"))
        assertFalse(isTrustedEchoApkUrl("https://echo.vibescape.club/echo.apk#fragment"))
    }
}
