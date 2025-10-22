package app.pluct.net

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object SimpleHttp {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    data class Response(val status: Int, val body: String)

    fun get(url: String, headers: Map<String, String> = emptyMap()): Response {
        val reqBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
        val res = client.newCall(reqBuilder.build()).execute()
        val body = res.body?.string() ?: ""
        val status = res.code
        res.close()
        return Response(status, body)
    }
}


