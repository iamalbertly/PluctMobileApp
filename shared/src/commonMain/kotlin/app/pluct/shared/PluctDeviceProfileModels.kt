package app.pluct.shared

import kotlinx.serialization.Serializable

@Serializable
data class PluctDeviceProfile(
    val deviceModel: String,
    val deviceType: String,
    val osName: String,
    val osVersion: String,
    val appVersion: String,
    val locale: String,
    val source: String
) {
    fun toApiPayload(): Map<String, String> = mapOf(
        "deviceModel" to deviceModel.take(120),
        "deviceType" to deviceType,
        "osName" to osName,
        "osVersion" to osVersion.take(32),
        "appVersion" to appVersion.take(32),
        "locale" to locale.take(16),
        "source" to source
    )

    fun stableFingerprint(): String {
        return toApiPayload().toSortedMap().entries.joinToString("|") { "${it.key}=${it.value}" }.hashCode().toString()
    }
}
