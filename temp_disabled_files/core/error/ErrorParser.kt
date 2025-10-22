package app.pluct.core.error

import org.json.JSONObject

object ErrorParser {
    fun fromEngineBody(body: String, defaultCode: String): ErrorEnvelope? = try {
        val json = JSONObject(body)
        if (json.optBoolean("ok") == false) {
            ErrorEnvelope(
                code = json.optString("code", defaultCode),
                message = json.optString("message", "Unknown error"),
                details = json.optJSONObject("details")?.let { obj ->
                    obj.keys().asSequence().associateWith { obj.opt(it) }
                } ?: emptyMap(),
                requestId = json.optString("requestId", null),
                source = "engine"
            )
        } else null
    } catch (_: Throwable) { null }
}
