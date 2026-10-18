package com.maxrave.data.repository

import club.vibescape.echo.contracts.EchoDiagnostics
import club.vibescape.echo.contracts.EchoOperation
import io.github.jan.supabase.exceptions.RestException

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
import live.vibescape.echo.EchoReleaseIdentity
import com.maxrave.common.AppIdentity

@Serializable
internal data class EchoReleaseMetadata(
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
    val created_at: String? = null,
)

internal fun EchoReleaseMetadata.toUpdateData(forceUpdate: Boolean) = UpdateData(
    tagName = version_name, releaseTime = created_at, body = changelog_markdown,
    apkUrl = apk_url, versionCode = version_code, apkSha256 = apk_sha256,
    apkArmv7Url = apk_armv7_url, apkArmv7Sha256 = apk_armv7_sha256,
    forceUpdate = forceUpdate,
)

/** V2 releases have a separate catalog; the V1 client reads app_versions without filtering. */
internal class UpdateRepositoryImpl(
    private val appIdentity: AppIdentity,
    private val supabaseProvider: () -> SupabaseClient,
) : UpdateRepository {
    override fun checkForEchoUpdate(currentVersionCode: Int): Flow<Resource<UpdateData>> = flow<Resource<UpdateData>> {
        try {
            EchoDiagnostics.started(EchoOperation.UpdateCheck)
            val channel = EchoReleaseIdentity.channelFor(appIdentity.applicationId, appIdentity.isDevBuild)
            if (channel == null) {
                EchoDiagnostics.succeeded(EchoOperation.UpdateCheck)
                emit(Resource.Success(UpdateData(tagName = "", releaseTime = null, body = "", versionCode = currentVersionCode)))
                return@flow
            }
            val releases = supabaseProvider().postgrest[EchoConfig.RELEASE_CATALOG_TABLE].select {
                filter { eq("application_id", appIdentity.applicationId); eq("release_channel", channel); gt("version_code", currentVersionCode) }
                order("version_code", Order.DESCENDING)
            }.decodeList<EchoReleaseMetadata>()
            // Retain the identity boundary even if a backend responds with an unfiltered row.
            val matching = releases.filter { it.application_id == appIdentity.applicationId && it.release_channel == channel }
            val latest = matching.firstOrNull()
            EchoDiagnostics.succeeded(EchoOperation.UpdateCheck)
            emit(Resource.Success(latest?.toUpdateData(matching.any(EchoReleaseMetadata::force_update))
                ?: UpdateData(tagName = "", releaseTime = null, body = "", versionCode = currentVersionCode)))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            EchoDiagnostics.failed(EchoOperation.UpdateCheck, failure, (failure as? RestException)?.statusCode)
            // No backend configuration or an unavailable catalog must not prevent playback.
            emit(Resource.Error("Echo updates are unavailable. Try again later."))
        }
    }.flowOn(Dispatchers.IO)

    // Legacy preference/method names survive migrations, but never query another app's catalog.
    override fun checkForGithubReleaseUpdate() = checkForEchoUpdate(0)
    override fun checkForFdroidUpdate() = checkForEchoUpdate(0)
    override fun getFdroidSigningKeys(): Flow<Resource<List<String>>> = flow { emit(Resource.Success(emptyList())) }
}
