package com.maxrave.data.repository

import com.maxrave.domain.data.model.update.UpdateData
import com.maxrave.domain.repository.UpdateRepository
import com.maxrave.domain.utils.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import live.vibescape.echo.EchoConfig

@Serializable
private data class EchoReleaseMetadata(
    val version_code: Int,
    val version_name: String,
    val changelog_markdown: String = "",
    val apk_url: String,
    val apk_sha256: String = "",
    val apk_armv7_url: String? = null,
    val apk_armv7_sha256: String? = null,
    val application_id: String,
    val release_channel: String,
    val force_update: Boolean = false,
)

/** V2 releases have a separate catalog; the V1 client reads app_versions without filtering. */
internal class UpdateRepositoryImpl(private val supabaseProvider: () -> SupabaseClient) : UpdateRepository {
    override fun checkForEchoUpdate(currentVersionCode: Int): Flow<Resource<UpdateData>> = flow<Resource<UpdateData>> {
        try {
            val releases = supabaseProvider().postgrest[EchoConfig.RELEASE_CATALOG_TABLE].select {
                filter { eq("application_id", EchoConfig.APPLICATION_ID); eq("release_channel", EchoConfig.RELEASE_CHANNEL); gt("version_code", currentVersionCode) }
                order("version_code", Order.DESCENDING)
            }.decodeList<EchoReleaseMetadata>()
            val latest = releases.firstOrNull()
            emit(Resource.Success(latest?.let {
                UpdateData(tagName = it.version_name, releaseTime = null, body = it.changelog_markdown,
                    apkUrl = it.apk_url, versionCode = it.version_code, apkSha256 = it.apk_sha256,
                    apkArmv7Url = it.apk_armv7_url, apkArmv7Sha256 = it.apk_armv7_sha256,
                    forceUpdate = releases.any(EchoReleaseMetadata::force_update))
            } ?: UpdateData(tagName = "", releaseTime = null, body = "", versionCode = currentVersionCode)))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // No backend configuration or an unavailable catalog must not prevent playback.
            emit(Resource.Error("Echo updates are unavailable. Try again later."))
        }
    }.flowOn(Dispatchers.IO)

    // Legacy preference/method names survive migrations, but never query another app's catalog.
    override fun checkForGithubReleaseUpdate() = checkForEchoUpdate(0)
    override fun checkForFdroidUpdate() = checkForEchoUpdate(0)
    override fun getFdroidSigningKeys(): Flow<Resource<List<String>>> = flow { emit(Resource.Success(emptyList())) }
}
