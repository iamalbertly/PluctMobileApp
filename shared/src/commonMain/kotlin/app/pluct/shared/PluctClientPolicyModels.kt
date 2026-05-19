package app.pluct.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class PluctClientPolicy(
    val minSupportedVersion: String? = null,
    val recommendedVersion: String? = null,
    val latestVersion: String? = null,
    val minimumVersionCode: Int? = null,
    val latestVersionCode: Int? = null,
    val updateMode: String? = null,
    @SerialName("disableTranscribeSubmit")
    val disableTranscribeSubmit: Boolean = false,
    val playStoreUrl: String? = null,
    val fallbackUrl: String? = null,
    val apkDownloadUrl: String? = null,
    val iosUrl: String? = null,
    val message: String? = null
)

object PluctClientPolicyModels {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String): PluctClientPolicy? {
        if (raw.isBlank()) return null
        return runCatching { json.decodeFromString<PluctClientPolicy>(raw) }.getOrNull()
    }

    fun isTranscribeDisabled(raw: String): Boolean {
        if (raw.isBlank()) return false
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val primitive = obj["disableTranscribeSubmit"]?.jsonPrimitive
                ?: obj["featureFlags"]?.jsonObject?.get("disableTranscribeSubmit")?.jsonPrimitive
                ?: return false
            primitive.booleanOrNull == true || primitive.content.equals("true", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    fun isHardUpdateRequired(raw: String, currentVersion: String): Boolean {
        if (raw.isBlank()) return false
        return try {
            val policy = parse(raw) ?: return false
            policy.updateMode.equals("hard", ignoreCase = true) &&
                compareVersions(currentVersion, policy.minSupportedVersion ?: policy.recommendedVersion ?: "") < 0
        } catch (_: Exception) {
            false
        }
    }

    fun isHardUpdateRequiredByCode(raw: String, currentVersionCode: Int): Boolean {
        if (raw.isBlank()) return false
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val rootMinimum = obj["minimumVersionCode"]?.jsonPrimitive?.content?.toIntOrNull()
            val androidMinimum = obj["platforms"]?.jsonObject
                ?.get("android")?.jsonObject
                ?.get("minimumVersionCode")?.jsonPrimitive?.content?.toIntOrNull()
            val forceUpdate = obj["platforms"]?.jsonObject
                ?.get("android")?.jsonObject
                ?.get("forceUpdate")?.jsonPrimitive?.booleanOrNull
                ?: parse(raw)?.updateMode.equals("hard", ignoreCase = true)
            val minimum = androidMinimum ?: rootMinimum ?: return false
            forceUpdate && minimum > currentVersionCode
        } catch (_: Exception) {
            false
        }
    }

    fun updateUrl(raw: String): String? {
        val policy = parse(raw) ?: return null
        return listOf(policy.apkDownloadUrl, policy.fallbackUrl, policy.playStoreUrl, policy.iosUrl)
            .firstOrNull { !it.isNullOrBlank() }
    }

    private fun compareVersions(left: String, right: String): Int {
        if (right.isBlank()) return 0
        val l = left.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
        val r = right.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
        val max = maxOf(l.size, r.size)
        for (i in 0 until max) {
            val lv = l.getOrElse(i) { 0 }
            val rv = r.getOrElse(i) { 0 }
            if (lv != rv) return lv.compareTo(rv)
        }
        return 0
    }
}
