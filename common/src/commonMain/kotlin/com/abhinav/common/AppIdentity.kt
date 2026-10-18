package com.abhinav.common

import live.vibescape.echo.EchoConfig

/** Supplied by the application entry point so build variants retain their real identity. */
data class AppIdentity(
    val applicationId: String,
    val versionName: String,
    val platform: String,
    /** Debug builds and Gradle runs. They read the test remote config and keep their cached copy of it apart. */
    val isDevBuild: Boolean = false,
    /** Optional upstream service; forks can disable it without deleting its implementation. */
    val communityLyricsEnabled: Boolean = true,
) {
    val userAgent: String
        get() = "${EchoConfig.APP_NAME}/$versionName ($applicationId; $platform)"
}
