package app.pluct.net

import app.pluct.core.error.ErrorEnvelope
import app.pluct.core.error.ErrorParser
import app.pluct.ui.error.ErrorCenter
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.net.SocketTimeoutException
import javax.inject.Inject

class PluctErrorInterceptor @Inject constructor(
    private val errorCenter: ErrorCenter
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            val resp = chain.proceed(chain.request())
            if (!resp.isSuccessful) {
                val bodyStr = resp.body?.string().orEmpty()
                val code = when(resp.code) {
                    401 -> "AUTH_401"
                    402 -> "CREDITS_402"
                    403 -> "SCOPE_403"
                    404 -> "NOT_FOUND_404"
                    429 -> "RATE_429"
                    500 -> "SERVER_500"
                    else -> "HTTP_${resp.code}"
                }
                val env = ErrorParser.fromEngineBody(bodyStr, code)
                    ?: ErrorEnvelope(code, "Request failed (${resp.code})")
                errorCenter.emit(env)
                // Re-create body since .string() consumed it
                return resp.newBuilder()
                    .body(bodyStr.toResponseBody(resp.body?.contentType()))
                    .build()
            }
            resp
        } catch (t: Throwable) {
            val env = when (t) {
                is SocketTimeoutException -> ErrorEnvelope("NET_TIMEOUT", "Connection timed out")
                else -> ErrorEnvelope("NET_IO", t.message ?: "Network error")
            }
            errorCenter.emit(env)
            throw t
        }
    }
}
