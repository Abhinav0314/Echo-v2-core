package live.vibescape.echo

/** Retained for callers that do not have Echo's configured backend origin. */
fun isTrustedEchoApkUrl(url: String): Boolean = isTrustedEchoApkUrl(url, null)

/** Public release storage is trusted only for this build's configured project and v2 folder. */
fun isTrustedEchoApkUrl(url: String, configuredSupabaseUrl: String?): Boolean = runCatching {
    val uri = com.eygraber.uri.Uri.parse(url)
    if (uri.scheme != "https" || uri.userInfo != null || uri.port !in listOf(-1, 443) || uri.fragment != null) {
        return@runCatching false
    }
    val host = uri.host?.lowercase()
    if (host in setOf(EchoConfig.HOST, "github.com", "objects.githubusercontent.com", "release-assets.githubusercontent.com")) {
        return@runCatching true
    }
    val backend = configuredSupabaseUrl?.let { com.eygraber.uri.Uri.parse(it) } ?: return@runCatching false
    val backendHost = backend.host?.lowercase() ?: return@runCatching false
    if (backend.scheme != "https" || backend.userInfo != null || backend.port !in listOf(-1, 443) ||
        backend.fragment != null || backend.encodedQuery != null ||
        backend.encodedPath !in listOf(null, "", "/") || !backendHost.endsWith(".supabase.co") || host != backendHost) {
        return@runCatching false
    }
    // Match encoded paths directly. Encoded traversal/separators, signed URLs and other buckets
    // must not inherit trust from the project host. Release workflow writes direct APK children.
    uri.encodedQuery == null && uri.encodedPath?.matches(
        Regex("/storage/v1/object/public/releases/echo-v2/[A-Za-z0-9][A-Za-z0-9._-]*\\.apk")
    ) == true
}.getOrDefault(false)
