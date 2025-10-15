package app.pluct.net

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset
import java.util.UUID
import android.util.Log

class PluctHttpTraceInterceptor : Interceptor {
    private val TAG = "PLUCT_HTTP"
    private val UTF8: Charset = Charsets.UTF_8

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val id = UUID.randomUUID().toString()

        var bodyPreview = ""
        req.body?.let { body ->
            try {
                val buffer = Buffer()
                body.writeTo(buffer)
                val src = buffer.clone().readString(UTF8)
                bodyPreview = if (src.length > 4096) src.substring(0, 4096) + "…(truncated)" else src
            } catch (_: Throwable) {}
        }

        val redactedHeaders = req.headers.toMultimap().mapValues { e ->
            e.value.map { v -> if (e.key.equals("authorization", true)) v.replace(Regex("(Bearer)\\s+\\S+"), "$1 <redacted>") else v }
        }

        Log.i(TAG, buildJsonLine(mapOf(
            "event" to "request",
            "id" to id,
            "method" to req.method,
            "url" to req.url.toString(),
            "headers" to redactedHeaders,
            "bodyPreview" to bodyPreview
        )))

        val t0 = System.nanoTime()
        val resp = chain.proceed(req)
        val t1 = System.nanoTime()
        val durMs = ((t1 - t0) / 1e6).toLong()

        val respBody = resp.body
        val respBytes = try { respBody?.bytes() } catch (_: Throwable) { null }
        val respText = if (respBytes != null) String(respBytes, UTF8) else ""
        val bodyText = if (respText.length > 8192) respText.substring(0, 8192) + "…(truncated)" else respText
        val newResp = resp.newBuilder()
            .body(okhttp3.ResponseBody.create(respBody?.contentType(), respBytes ?: ByteArray(0)))
            .build()

        val respHeaders = resp.headers.toMultimap()

        Log.i(TAG, buildJsonLine(mapOf(
            "event" to "response",
            "id" to id,
            "code" to resp.code,
            "url" to req.url.toString(),
            "durationMs" to durMs,
            "headers" to respHeaders,
            "bodyPreview" to bodyText
        )))

        return newResp
    }

    private fun buildJsonLine(map: Map<String, Any?>): String {
        fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n","\\n")
        fun any(v: Any?): String = when (v) {
            null -> "null"
            is String -> "\"${esc(v)}\""
            is Number, is Boolean -> v.toString()
            is Map<*, *> -> v.entries.joinToString(prefix="{", postfix="}") { "\"${esc(it.key.toString())}\":${any(it.value)}" }
            is Iterable<*> -> v.joinToString(prefix="[", postfix="]") { any(it) }
            else -> "\"${esc(v.toString())}\""
        }
        return any(map)
    }
}


