package app.pluct.data

import android.util.Log
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import app.pluct.core.log.PluctLogger
import app.pluct.core.error.ErrorEnvelope

/**
 * Pluct-Data-BusinessEngine-03Balance - Credit balance functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation][CoreResponsibility]
 */
class PluctBusinessEngineBalance(
    private val baseUrl: String,
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "PluctBusinessEngineBalance"
    }

    /**
     * Get user credit balance
     */
    suspend fun getCreditBalance(userJwt: String): BusinessEngineBalanceResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "💰 Getting credit balance...")
                val request = Request.Builder()
                    .url("$baseUrl/v1/credits/balance")
                    .get()
                    .addHeader("Authorization", "Bearer $userJwt")
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val userId = json.optString("userId", "")
                    val balance = json.optInt("balance", 0)
                    val updatedAt = json.optString("updatedAt", "")
                    
                    PluctLogger.logBusinessEngineCall("get_balance", true, 0, mapOf(
                        "userId" to userId,
                        "balance" to balance
                    ))

                    BusinessEngineBalanceResult(
                        userId = userId,
                        balance = balance,
                        updatedAt = updatedAt,
                        responseTime = 0
                    )
                } else {
                    PluctLogger.logBusinessEngineCall("get_balance", false, 0, mapOf(
                        "error" to "HTTP ${response.code}",
                        "body" to responseBody
                    ))
                    
                    BusinessEngineBalanceResult(
                        userId = "",
                        balance = 0,
                        updatedAt = "",
                        responseTime = 0,
                        error = "HTTP ${response.code}: $responseBody"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Balance check failed", e)
                PluctLogger.logError(ErrorEnvelope(
                    code = "BALANCE_CHECK_FAILED",
                    message = "Balance check failed: ${e.message}",
                    source = "client",
                    context = "balance_check"
                ))
                
                BusinessEngineBalanceResult(
                    userId = "",
                    balance = 0,
                    updatedAt = "",
                    responseTime = 0,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
}

data class BusinessEngineBalanceResult(
    val userId: String,
    val balance: Int,
    val updatedAt: String,
    val responseTime: Long,
    val error: String? = null
)
