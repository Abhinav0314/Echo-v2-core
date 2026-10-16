package live.vibescape.echo

/** Compatibility distribution only; branding and the production certificate remain Echo's. */
object EchoReleaseIdentity {
    const val VIVO_APPLICATION_ID = "com.jio.media.jiobeats"
    const val VIVO_CHANNEL = "echo-vivo"

    fun isReleasePackage(applicationId: String): Boolean =
        applicationId == EchoConfig.APPLICATION_ID || applicationId == VIVO_APPLICATION_ID

    /** Unknown/development packages cannot read a production installation's update catalog. */
    fun channelFor(applicationId: String, isDevBuild: Boolean = false): String? = when {
        isDevBuild -> null
        applicationId == EchoConfig.APPLICATION_ID -> EchoConfig.RELEASE_CHANNEL
        applicationId == VIVO_APPLICATION_ID -> VIVO_CHANNEL
        else -> null
    }
}
