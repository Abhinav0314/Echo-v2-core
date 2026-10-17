package com.maxrave.media3.service

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.os.Bundle
import android.os.Process
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MediaControllerAccessTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private fun controller(name: String = "untrusted.app", uid: Int = Process.myUid() + 1000, trusted: Boolean = false) =
        MediaSession.ControllerInfo.createTestOnlyControllerInfo(name, 123, uid, 1, 1, trusted, Bundle.EMPTY, true)

    @Test fun rejectsUntrustedAppsAndSpoofedCarNames() {
        val access = MediaControllerAccess(context)
        assertFalse(access.allows(controller()))
        assertFalse(access.allows(controller("com.google.android.projection.gearhead")))
    }

    @Test fun rejectsCarPackageWithAnUnrecognizedSigningCertificate() {
        val uid = Process.myUid() + 1001
        val name = "com.google.android.projection.gearhead"
        shadowOf(context.packageManager).installPackage(PackageInfo().apply {
            packageName = name
            applicationInfo = ApplicationInfo().apply { packageName = name; this.uid = uid }
            @Suppress("DEPRECATION")
            signatures = arrayOf(Signature("001122334455"))
        })
        assertFalse(MediaControllerAccess(context).allows(controller(name, uid)))
    }

    @Test fun allowsOwnBrowserAndUserTrustedSystemControllers() {
        val access = MediaControllerAccess(context)
        assertTrue(access.allows(controller(uid = Process.myUid())))
        assertTrue(access.allows(controller("system.controller", trusted = true)))
    }
}
