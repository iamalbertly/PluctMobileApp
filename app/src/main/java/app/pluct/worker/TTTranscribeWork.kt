package app.pluct.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.pluct.data.BusinessEngineClient
import app.pluct.data.EngineError
import kotlinx.coroutines.flow.first
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TTTranscribeWork @Inject constructor(
    @ApplicationContext ctx: Context,
    params: WorkerParameters,
    private val businessEngineClient: BusinessEngineClient
) : CoroutineWorker(ctx, params) {

    private suspend fun stage(s: String, url: String, reqId: String? = null, msg: String? = null, pct: Int? = null) {
        setProgress(workDataOf("stage" to s, "percent" to (pct ?: -1)))
        Log.i("TTT", "stage=$s url=$url reqId=${reqId ?: "-"} msg=${msg ?: ""}")
    }

    override suspend fun doWork(): Result {
        val videoUrl = inputData.getString("url") ?: return Result.failure()
        
        return try {
            // Step 1: Health check
            Log.i("TTT", "stage=HEALTH_CHECK url=$videoUrl reqId=- msg=checking")
            val health = businessEngineClient.health()
            if (!health.isHealthy) {
                Log.e("TTT", "Business Engine health check failed")
                return Result.retry()
            }
            Log.i("TTT", "stage=HEALTH_CHECK url=$videoUrl reqId=- msg=success")
            
            // Step 2: Check balance
            Log.i("TTT", "stage=CREDIT_CHECK url=$videoUrl reqId=- msg=checking")
            val userJwt = inputData.getString("userJwt") ?: ""
            if (userJwt.isEmpty()) {
                Log.e("TTT", "No user JWT provided for balance check")
                return Result.failure()
            }
            val balance = businessEngineClient.balance(userJwt)
            if (balance.balance <= 0) {
                Log.e("TTT", "Insufficient credits: ${balance.balance}")
                return Result.retry()
            }
            Log.i("TTT", "stage=CREDIT_CHECK url=$videoUrl reqId=- msg=success")
            
            // Step 3: Vend token (requires user JWT; obtain from app session)
            Log.i("TTT", "stage=VENDING_TOKEN url=$videoUrl reqId=- msg=requesting")
            val vendResult = try {
                val userJwt = inputData.getString("userJwt") ?: ""
                if (userJwt.isEmpty()) {
                    Log.e("TTT", "No user JWT provided for token vending")
                    return Result.failure()
                }
                val reqId = java.util.UUID.randomUUID().toString()
                businessEngineClient.vendShortToken(userJwt, reqId)
            } catch (e: EngineError) {
                if (e is EngineError.InsufficientCredits) {
                    Log.e("TTT", "Insufficient credits for token vending")
                    return Result.retry()
                } else if (e is EngineError.RateLimited) {
                    Log.e("TTT", "Rate limited for token vending")
                    return Result.retry()
                } else {
                    Log.e("TTT", "Token vending failed: ${e.message}")
                    return Result.retry()
                }
            }
            Log.i("TTT", "stage=VENDING_TOKEN url=$videoUrl reqId=- msg=success")
            
            // Step 4: Start transcription
            Log.i("TTT", "stage=TTTRANSCRIBE_CALL url=$videoUrl reqId=- msg=requesting")
            val requestId = try {
                businessEngineClient.transcribe(videoUrl, vendResult.token)
            } catch (e: EngineError) {
                if (e is EngineError.InvalidUrl) {
                    Log.e("TTT", "Invalid URL: $videoUrl")
                    return Result.failure()
                } else if (e is EngineError.Auth) {
                    Log.e("TTT", "Authentication failed")
                    return Result.retry()
                } else {
                    Log.e("TTT", "Transcription failed: ${e.message}")
                    return Result.retry()
                }
            }
            Log.i("TTT", "stage=TTTRANSCRIBE_CALL url=$videoUrl reqId=$requestId msg=success")
            
            // Step 5: Poll status
            Log.i("TTT", "stage=STATUS_POLLING url=$videoUrl reqId=$requestId msg=requesting")
            val finalStatus = businessEngineClient.pollStatus(requestId).first { status ->
                status.phase == "COMPLETED" || status.phase == "FAILED"
            }
            
            if (finalStatus.phase == "COMPLETED") {
                Log.i("TTT", "stage=COMPLETED url=$videoUrl reqId=$requestId msg=success")
                Result.success()
            } else {
                Log.e("TTT", "Transcription failed: ${finalStatus.note}")
                Result.retry()
            }
            
        } catch (e: Exception) {
            Log.e("TTT", "Worker error: ${e.message}")
            Result.retry()
        }
    }
}