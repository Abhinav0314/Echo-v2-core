package live.vibescape.echo

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EchoUpdatePolicyTest {
    private val backend = "https://owned-project.supabase.co"
    private val releasePath = "/storage/v1/object/public/releases/echo-v2/echo-2.0.0.apk"

    @Test fun ownPublicReleaseFolderAcceptsBothPublishedAndroidVariants() {
        assertTrue(isTrustedEchoApkUrl(backend + releasePath, backend))
        assertTrue(isTrustedEchoApkUrl("$backend:443${releasePath.replace(".apk", "-armv7.apk")}", "$backend/"))
        assertFalse(isTrustedEchoApkUrl(backend + releasePath))
        for (suffix in listOf("", "-armv7", "-vivo")) {
            assertTrue(isTrustedEchoApkUrl("$backend/storage/v1/object/public/releases/echo-v2/build-2000010-echo-2.0.0$suffix.apk", backend))
        }
    }

    @Test fun otherProjectsBucketsAndSignedAssetsCannotBorrowProjectTrust() {
        for (url in listOf(
            "https://other-project.supabase.co$releasePath",
            "https://owned-project.supabase.co.attacker.org$releasePath",
            backend + releasePath.replace("/releases/", "/avatars/"),
            backend + releasePath.replace("/echo-v2/", "/echo-v1/"),
            backend + releasePath.replace("/public/", "/sign/"),
            "$backend$releasePath?token=opaque",
            "$backend$releasePath?download=echo.apk",
            "https://user@owned-project.supabase.co$releasePath",
            "$backend:444$releasePath",
            "$backend$releasePath#fragment",
        )) assertFalse(isTrustedEchoApkUrl(url, backend), url)
    }

    @Test fun pathTraversalAndEncodedSeparatorsNeverReachAnotherStorageObject() {
        for (suffix in listOf(
            "../echo.apk", "%2e%2e/echo.apk", "%252e%252e/echo.apk", "folder%2fecho.apk",
            "folder/echo.apk", "folder\\echo.apk", "echo.apk%3Ftoken=opaque", "", "echo.zip",
        )) assertFalse(isTrustedEchoApkUrl("$backend/storage/v1/object/public/releases/echo-v2/$suffix", backend), suffix)
    }

    @Test fun malformedOrUnrelatedBackendConfigurationCannotExtendExistingTrust() {
        for (configuration in listOf(null, "", "http://owned-project.supabase.co", "$backend/path", "$backend?key=value", "https://user@owned-project.supabase.co", "https://attacker.org")) {
            assertFalse(isTrustedEchoApkUrl(backend + releasePath, configuration))
        }
        assertTrue(isTrustedEchoApkUrl("https://echo.vibescape.club/echo-v2.apk", backend))
        assertTrue(isTrustedEchoApkUrl("https://github.com/Abhinav0314/Echo-App/releases/download/echo-v2.0.0/echo.apk", backend))
    }
}
