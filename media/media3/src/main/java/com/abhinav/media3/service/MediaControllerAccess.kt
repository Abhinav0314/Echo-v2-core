package com.abhinav.media3.service

import android.content.Context
import android.os.Process
import androidx.car.app.HostInfo
import androidx.car.app.validation.HostValidator
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession

/** Admission uses verified UIDs/permissions or the car library's signature validation. */
@UnstableApi
internal class MediaControllerAccess(context: Context) {
    private val packageManager = context.packageManager
    private val hostValidator by lazy {
        HostValidator.Builder(context.applicationContext)
            .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
            .build()
    }

    fun allows(controller: MediaSession.ControllerInfo): Boolean {
        if (controller.uid == Process.myUid() || controller.isTrusted) return true
        if (controller.uid < 0 || controller.packageName !in packageManager.getPackagesForUid(controller.uid).orEmpty()) return false
        return hostValidator.isValidHost(HostInfo(controller.packageName, controller.uid))
    }
}
