package com.maxrave.domain.data.model.update

data class UpdateData(
    val tagName: String,
    val releaseTime: String?,
    val body: String,
    val apkUrl: String? = null,
    val versionCode: Int? = null,
    val apkSha256: String? = null,
    val forceUpdate: Boolean = false,
    val apkArmv7Url: String? = null,
    val apkArmv7Sha256: String? = null,
)
/** One native architecture per APK; absence of a compatible artifact must not install another ABI. */
data class EchoUpdateApk(val url: String, val sha256: String, val abi: String)

fun UpdateData.apkForSupportedAbis(supportedAbis: List<String>): EchoUpdateApk {
    val (url, checksum, abi) = when {
        "arm64-v8a" in supportedAbis -> Triple(apkUrl, apkSha256, "arm64-v8a")
        "armeabi-v7a" in supportedAbis -> Triple(apkArmv7Url, apkArmv7Sha256, "armeabi-v7a")
        else -> error("Echo updates are available for ARM Android devices only")
    }
    check(!url.isNullOrBlank() && checksum?.matches(Regex("[a-fA-F0-9]{64}")) == true) {
        "A verified Echo update is not available for $abi"
    }
    return EchoUpdateApk(checkNotNull(url), checkNotNull(checksum).lowercase(), abi)
}
