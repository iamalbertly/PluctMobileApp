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
    val message: String? = null,
    val messageShort: String? = null,
    val messageDetail: String? = null
)

object PluctClientPolicyModels {
    const val HARD_UPDATE_CTA = "Update required. Tap Update."
    const val SOFT_UPDATE_CTA = "Update ready. Faster and safer."

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String): PluctClientPolicy? {
        if (raw.isBlank()) return null
        return runCatching { json.decodeFromString<PluctClientPolicy>(raw) }.getOrNull()
    }

    fun isTranscribeDisabled(raw: String): Boolean {
        if (raw.isBlank()) return false
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val featureSubmit = obj["features"]?.jsonObject
                ?.get("transcriptionSubmit")?.jsonPrimitive
            val primitive = obj["disableTranscribeSubmit"]?.jsonPrimitive
                ?: obj["featureFlags"]?.jsonObject?.get("disableTranscribeSubmit")?.jsonPrimitive
            if (featureSubmit?.booleanOrNull == false || featureSubmit?.content.equals("false", ignoreCase = true)) {
                return true
            }
            primitive?.booleanOrNull == true || primitive?.content.equals("true", ignoreCase = true)
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
            val mode = obj["updateMode"]?.jsonPrimitive?.content?.lowercase() ?: "soft"
            val rootMinimum = obj["minimumVersionCode"]?.jsonPrimitive?.content?.toIntOrNull()
            val androidMinimum = obj["platforms"]?.jsonObject
                ?.get("android")?.jsonObject
                ?.get("minimumVersionCode")?.jsonPrimitive?.content?.toIntOrNull()
            val rootLatest = obj["latestVersionCode"]?.jsonPrimitive?.content?.toIntOrNull()
            val android = obj["platforms"]?.jsonObject?.get("android")?.jsonObject
            val androidLatest = android?.get("latestVersionCode")?.jsonPrimitive?.content?.toIntOrNull()
            val androidForce = android?.get("forceUpdate")?.jsonPrimitive?.booleanOrNull == true
            val minimum = androidMinimum ?: rootMinimum
            val latest = androidLatest ?: rootLatest
            minimum?.let { if (it > currentVersionCode) return true }
            (mode == "hard" || androidForce) && latest != null && latest > currentVersionCode
        } catch (_: Exception) {
            false
        }
    }

    fun isSoftUpdateAvailableByCode(raw: String, currentVersionCode: Int): Boolean {
        if (raw.isBlank()) return false
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val mode = obj["updateMode"]?.jsonPrimitive?.content?.lowercase() ?: "soft"
            if (mode == "none" || isHardUpdateRequiredByCode(raw, currentVersionCode)) return false
            val rootLatest = obj["latestVersionCode"]?.jsonPrimitive?.content?.toIntOrNull()
            val androidLatest = obj["platforms"]?.jsonObject
                ?.get("android")?.jsonObject
                ?.get("latestVersionCode")?.jsonPrimitive?.content?.toIntOrNull()
            val latest = androidLatest ?: rootLatest ?: return false
            latest > currentVersionCode
        } catch (_: Exception) {
            false
        }
    }

    fun updateUrl(raw: String): String? {
        val nestedApkUrl = runCatching {
            json.parseToJsonElement(raw).jsonObject["platforms"]?.jsonObject
                ?.get("android")?.jsonObject
                ?.get("apkUrl")?.jsonPrimitive?.content
        }.getOrNull()
        val policy = parse(raw) ?: return null
        return listOf(nestedApkUrl, policy.apkDownloadUrl, policy.fallbackUrl, policy.playStoreUrl, policy.iosUrl)
            .firstOrNull { !it.isNullOrBlank() }
    }

    fun updateMessage(raw: String, hardUpdateRequired: Boolean, softUpdateAvailable: Boolean): String {
        val policy = parse(raw)
        return policy?.messageDetail
            ?: policy?.message
            ?: policy?.messageShort
            ?: when {
                hardUpdateRequired -> "Update Pluct to keep TikTok to text working."
                softUpdateAvailable -> "A faster Pluct update is ready."
                else -> "Latest Pluct is ready."
            }
    }

    fun ctaMessage(hardUpdateRequired: Boolean, softUpdateAvailable: Boolean): String? {
        return when {
            hardUpdateRequired -> HARD_UPDATE_CTA
            softUpdateAvailable -> SOFT_UPDATE_CTA
            else -> null
        }
    }

    fun isHardUpdateCta(message: String?): Boolean {
        return message?.equals(HARD_UPDATE_CTA, ignoreCase = true) == true ||
            message?.contains("Update required", ignoreCase = true) == true
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
