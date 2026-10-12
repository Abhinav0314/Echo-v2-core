package com.maxrave.domain.data.model.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EchoUpdateApkTest {
    private val release = UpdateData("2.0.0", null, "", apkUrl = "https://example.test/echo.apk",
        apkSha256 = "A".repeat(64), apkArmv7Url = "https://example.test/echo-armv7.apk", apkArmv7Sha256 = "b".repeat(64))

    @Test fun arm64IsPreferredWhenDeviceSupportsBothArchitectures() {
        val apk = release.apkForSupportedAbis(listOf("armeabi-v7a", "arm64-v8a"))
        assertEquals("arm64-v8a", apk.abi)
        assertEquals(release.apkUrl, apk.url)
        assertEquals("a".repeat(64), apk.sha256)
    }

    @Test fun thirtyTwoBitDeviceUsesMatchingUrlAndChecksum() {
        val apk = release.apkForSupportedAbis(listOf("armeabi-v7a", "armeabi"))
        assertEquals("armeabi-v7a", apk.abi)
        assertEquals(release.apkArmv7Url, apk.url)
        assertEquals(release.apkArmv7Sha256, apk.sha256)
    }

    @Test fun missingArmv7NeverFallsBackToIncompatibleArm64() {
        assertFailsWith<IllegalStateException> {
            release.copy(apkArmv7Url = null, apkArmv7Sha256 = null).apkForSupportedAbis(listOf("armeabi-v7a"))
        }
    }

    @Test fun partialOrInvalidArtifactIsRejected() {
        for (checksum in listOf(null, "", "not-a-checksum", "z".repeat(64))) {
            assertFailsWith<IllegalStateException> {
                release.copy(apkArmv7Sha256 = checksum).apkForSupportedAbis(listOf("armeabi-v7a"))
            }
        }
        assertFailsWith<IllegalStateException> { release.copy(apkUrl = "").apkForSupportedAbis(listOf("arm64-v8a")) }
    }

    @Test fun unsupportedDeviceDoesNotReceiveArmApk() {
        assertFailsWith<IllegalStateException> { release.apkForSupportedAbis(listOf("x86_64", "x86")) }
    }
}
