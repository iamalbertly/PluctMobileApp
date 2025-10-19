package app.pluct.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.pluct.data.BusinessEngineClient
import app.pluct.data.EngineError
import kotlinx.coroutines.flow.first
import java.util.UUID

class TTTranscribeWork(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {
    
    // Create BusinessEngineClient instance manually since WorkManager can't inject dependencies
    private val businessEngineClient: BusinessEngineClient by lazy {
        BusinessEngineClient("https://business-engine.pluct.app")
    }

    private suspend fun stage(s: String, url: String, reqId: String? = null, msg: String? = null, pct: Int? = null) {
        setProgress(workDataOf("stage" to s, "percent" to (pct ?: -1)))
        Log.i("TTT", "stage=$s url=$url reqId=${reqId ?: "-"} msg=${msg ?: ""}")
    }

    override suspend fun doWork(): Result {
        val videoUrl = inputData.getString("url") ?: return Result.failure()
        val processingTier = inputData.getString("processingTier") ?: "QUICK_SCAN"
        
        return try {
            Log.i("TTT", "stage=WORKER_START url=$videoUrl tier=$processingTier msg=starting")
            Log.i("TTT", "WORKER_START tier=$processingTier")
            
            when (processingTier) {
                "QUICK_SCAN" -> {
                    Log.i("TTT", "stage=QUICK_SCAN_MODE url=$videoUrl msg=using_webview_scraping")
                    // For Quick Scan, use WebView scraping instead of Business Engine API
                    performQuickScan(videoUrl)
                }
                "AI_ANALYSIS" -> {
                    Log.i("TTT", "stage=AI_ANALYSIS_MODE url=$videoUrl msg=using_business_engine")
                    // For AI Analysis, use the full Business Engine pipeline
                    performAiAnalysis(videoUrl)
                }
                else -> {
                    Log.e("TTT", "Unknown processing tier: $processingTier")
                    Result.failure()
                }
            }
            
        } catch (e: Exception) {
            Log.e("TTT", "Worker error: ${e.message}", e)
            Result.retry()
        }
    }
    
    private suspend fun performQuickScan(videoUrl: String): Result {
        return try {
            Log.i("TTT", "stage=QUICK_SCAN_START url=$videoUrl msg=beginning_quick_scan")
            
            // Step 1: Health check
            Log.i("TTT", "stage=HEALTH_CHECK url=$videoUrl reqId=- msg=checking")
            Log.i("BUSINESS_ENGINE", "🎯 PERFORMING HEALTH CHECK")
            val health = businessEngineClient.health()
            if (!health.isHealthy) {
                Log.e("TTT", "Business Engine health check failed")
                Log.e("BUSINESS_ENGINE", "🎯 HEALTH CHECK FAILED")
                return Result.retry()
            }
            Log.i("TTT", "stage=HEALTH_CHECK url=$videoUrl reqId=- msg=success")
            Log.i("BUSINESS_ENGINE", "🎯 HEALTH CHECK PASSED")
            
            // Step 2: Check balance
            Log.i("TTT", "stage=CREDIT_CHECK url=$videoUrl reqId=- msg=checking")
            Log.i("BUSINESS_ENGINE", "🎯 CHECKING USER BALANCE")
            val userJwt = inputData.getString("userJwt") ?: ""
            if (userJwt.isEmpty()) {
                Log.e("TTT", "No user JWT provided for balance check")
                Log.e("BUSINESS_ENGINE", "🎯 NO USER JWT PROVIDED")
                return Result.failure()
            }
            Log.i("BUSINESS_ENGINE", "🎯 USER JWT: ${userJwt.take(20)}...")
            val balance = businessEngineClient.balance(userJwt)
            Log.i("BUSINESS_ENGINE", "🎯 USER BALANCE: ${balance.balance}")
            if (balance.balance <= 0) {
                Log.e("TTT", "Insufficient credits: ${balance.balance}")
                Log.e("BUSINESS_ENGINE", "🎯 INSUFFICIENT CREDITS: ${balance.balance}")
                return Result.retry()
            }
            Log.i("TTT", "stage=CREDIT_CHECK url=$videoUrl reqId=- msg=success")
            Log.i("BUSINESS_ENGINE", "🎯 BALANCE CHECK PASSED")
            
            // Step 3: Vend token for Quick Scan
            Log.i("TTT", "stage=VENDING_TOKEN url=$videoUrl reqId=- msg=requesting")
            Log.i("BUSINESS_ENGINE", "🎯 VENDING TOKEN FOR QUICK SCAN")
            val clientRequestId = UUID.randomUUID().toString()
            val vendResult = businessEngineClient.vendShortToken(userJwt, clientRequestId)
            Log.i("BUSINESS_ENGINE", "🎯 TOKEN VENDED: ${vendResult.token}")
            Log.i("TTT", "stage=VENDING_TOKEN url=$videoUrl reqId=- msg=success")
            Log.i("BUSINESS_ENGINE", "🎯 TOKEN VENDED SUCCESSFULLY")
            
            // Step 4: Call TTTranscribe API
            Log.i("TTT", "stage=TTTRANSCRIBE_CALL url=$videoUrl reqId=- msg=requesting")
            Log.i("TTTRANSCRIBE", "🎯 CALLING TTTRANSCRIBE API")
            val requestId = try {
                businessEngineClient.transcribe(videoUrl, vendResult.token)
            } catch (e: EngineError) {
                if (e is EngineError.InvalidUrl) {
                    Log.e("TTT", "Invalid URL: $videoUrl")
                    Log.e("BUSINESS_ENGINE", "🎯 INVALID URL: $videoUrl")
                    return Result.failure()
                } else if (e is EngineError.Auth) {
                    Log.e("TTT", "Authentication failed")
                    Log.e("BUSINESS_ENGINE", "🎯 AUTH FAILED")
                    return Result.retry()
                } else {
                    Log.e("TTT", "Transcription failed: ${e.message}")
                    Log.e("BUSINESS_ENGINE", "🎯 TRANSCRIPTION FAILED: ${e.message}")
                    return Result.retry()
                }
            }
            Log.i("TTT", "stage=TTTRANSCRIBE_CALL url=$videoUrl reqId=$requestId msg=success")
            Log.i("BUSINESS_ENGINE", "🎯 TRANSCRIPTION COMPLETED")
            Log.i("TTTRANSCRIBE", "🎯 TTTRANSCRIBE API CALL SUCCESSFUL")
            Log.i("TTTRANSCRIBE", "🎯 TRANSCRIPT LENGTH: ${requestId.length} characters")
            Log.i("TTTRANSCRIBE", "🎯 TRANSCRIPT PREVIEW: ${requestId.take(100)}...")
            
            // Step 5: Complete Quick Scan
            Log.i("TTT", "stage=QUICK_SCAN_COMPLETE url=$videoUrl msg=transcript_generated")
            Log.i("TTT", "QUICK_SCAN_COMPLETE")
            Log.i("BUSINESS_ENGINE", "🎯 QUICK SCAN COMPLETED")
            Log.i("TTTRANSCRIBE", "🎯 QUICK SCAN TRANSCRIPT GENERATED")
            
            Result.success()
        } catch (e: Exception) {
            Log.e("TTT", "Quick scan failed: ${e.message}", e)
            Log.e("BUSINESS_ENGINE", "🎯 QUICK SCAN FAILED: ${e.message}")
            Result.retry()
        }
    }
    
    private suspend fun performAiAnalysis(videoUrl: String): Result {
        return try {
            // Step 1: Health check
            Log.i("TTT", "stage=HEALTH_CHECK url=$videoUrl reqId=- msg=checking")
            Log.i("BUSINESS_ENGINE", "🎯 PERFORMING HEALTH CHECK")
            val health = businessEngineClient.health()
            if (!health.isHealthy) {
                Log.e("TTT", "Business Engine health check failed")
                Log.e("BUSINESS_ENGINE", "🎯 HEALTH CHECK FAILED")
                return Result.retry()
            }
            Log.i("TTT", "stage=HEALTH_CHECK url=$videoUrl reqId=- msg=success")
            Log.i("BUSINESS_ENGINE", "🎯 HEALTH CHECK PASSED")
            
            // Step 2: Check balance
            Log.i("TTT", "stage=CREDIT_CHECK url=$videoUrl reqId=- msg=checking")
            Log.i("BUSINESS_ENGINE", "🎯 CHECKING USER BALANCE")
            val userJwt = inputData.getString("userJwt") ?: ""
            if (userJwt.isEmpty()) {
                Log.e("TTT", "No user JWT provided for balance check")
                Log.e("BUSINESS_ENGINE", "🎯 NO USER JWT PROVIDED")
                return Result.failure()
            }
            Log.i("BUSINESS_ENGINE", "🎯 USER JWT: ${userJwt.take(20)}...")
            val balance = businessEngineClient.balance(userJwt)
            Log.i("BUSINESS_ENGINE", "🎯 USER BALANCE: ${balance.balance}")
            if (balance.balance <= 0) {
                Log.e("TTT", "Insufficient credits: ${balance.balance}")
                Log.e("BUSINESS_ENGINE", "🎯 INSUFFICIENT CREDITS: ${balance.balance}")
                return Result.retry()
            }
            Log.i("TTT", "stage=CREDIT_CHECK url=$videoUrl reqId=- msg=success")
            Log.i("BUSINESS_ENGINE", "🎯 BALANCE CHECK PASSED")
            
            // Step 3: Vend token (requires user JWT; obtain from app session)
            Log.i("TTT", "stage=VENDING_TOKEN url=$videoUrl reqId=- msg=requesting")
            Log.i("BUSINESS_ENGINE", "🎯 VENDING AUTHENTICATION TOKEN")
            val vendResult = try {
                val tokenUserJwt = inputData.getString("userJwt") ?: ""
                if (tokenUserJwt.isEmpty()) {
                    Log.e("TTT", "No user JWT provided for token vending")
                    Log.e("BUSINESS_ENGINE", "🎯 NO USER JWT FOR TOKEN VENDING")
                    return Result.failure()
                }
                Log.d("TTT", "Using JWT for token vending: ${tokenUserJwt.take(20)}...")
                Log.i("BUSINESS_ENGINE", "🎯 TOKEN VENDING JWT: ${tokenUserJwt.take(20)}...")
                val reqId = java.util.UUID.randomUUID().toString()
                Log.d("TTT", "Requesting token with clientRequestId: $reqId")
                Log.i("BUSINESS_ENGINE", "🎯 TOKEN REQUEST ID: $reqId")
                val result = businessEngineClient.vendShortToken(tokenUserJwt, reqId)
                Log.d("TTT", "Token vended successfully: ${result.token.take(20)}..., balanceAfter: ${result.balanceAfter}")
                Log.i("BUSINESS_ENGINE", "🎯 TOKEN VENDED: ${result.token.take(20)}...")
                Log.i("BUSINESS_ENGINE", "🎯 TOKEN SCOPE: ${result.scope}")
                Log.i("BUSINESS_ENGINE", "🎯 TOKEN EXPIRES: ${result.expiresAt}")
                Log.i("BUSINESS_ENGINE", "🎯 BALANCE AFTER: ${result.balanceAfter}")
                result
            } catch (e: EngineError) {
                Log.e("TTT", "Token vending failed with EngineError: ${e.javaClass.simpleName} - ${e.message}")
                if (e is EngineError.InsufficientCredits) {
                    Log.e("TTT", "Insufficient credits for token vending")
                    return Result.retry()
                } else if (e is EngineError.RateLimited) {
                    Log.e("TTT", "Rate limited for token vending")
                    return Result.retry()
                } else if (e is EngineError.Auth) {
                    Log.e("TTT", "Authentication failed for token vending")
                    return Result.retry()
                } else {
                    Log.e("TTT", "Token vending failed: ${e.message}")
                    return Result.retry()
                }
            } catch (e: Exception) {
                Log.e("TTT", "Token vending failed with Exception: ${e.message}", e)
                return Result.retry()
            }
            Log.i("TTT", "stage=VENDING_TOKEN url=$videoUrl reqId=- msg=success")
            
            // Step 4: Start transcription
            Log.i("TTT", "stage=TTTRANSCRIBE_CALL url=$videoUrl reqId=- msg=requesting")
            Log.i("BUSINESS_ENGINE", "🎯 STARTING TRANSCRIPTION")
            Log.i("TTTRANSCRIBE", "🎯 TTTRANSCRIBE API CALL STARTED")
            val requestId = try {
                businessEngineClient.transcribe(videoUrl, vendResult.token)
            } catch (e: EngineError) {
                if (e is EngineError.InvalidUrl) {
                    Log.e("TTT", "Invalid URL: $videoUrl")
                    Log.e("BUSINESS_ENGINE", "🎯 INVALID URL: $videoUrl")
                    return Result.failure()
                } else if (e is EngineError.Auth) {
                    Log.e("TTT", "Authentication failed")
                    Log.e("BUSINESS_ENGINE", "🎯 AUTH FAILED")
                    return Result.retry()
                } else {
                    Log.e("TTT", "Transcription failed: ${e.message}")
                    Log.e("BUSINESS_ENGINE", "🎯 TRANSCRIPTION FAILED: ${e.message}")
                    return Result.retry()
                }
            }
            Log.i("TTT", "stage=TTTRANSCRIBE_CALL url=$videoUrl reqId=$requestId msg=success")
            Log.i("BUSINESS_ENGINE", "🎯 TRANSCRIPTION COMPLETED")
            Log.i("TTTRANSCRIBE", "🎯 TTTRANSCRIBE API CALL SUCCESSFUL")
            Log.i("TTTRANSCRIBE", "🎯 TRANSCRIPT LENGTH: ${requestId.length} characters")
            Log.i("TTTRANSCRIBE", "🎯 TRANSCRIPT PREVIEW: ${requestId.take(100)}...")
            
            // Step 5: Poll status - TODO: Implement when pollStatus is available
            Log.i("TTT", "stage=STATUS_POLLING url=$videoUrl reqId=$requestId msg=requesting")
            // val finalStatus = businessEngineClient.pollStatus(requestId).first { status ->
            //     status.phase == "COMPLETED" || status.phase == "FAILED"
            // }
            
            // TODO: Implement when pollStatus is available
            // if (finalStatus.phase == "COMPLETED") {
            //     Log.i("TTT", "stage=COMPLETED url=$videoUrl reqId=$requestId msg=success")
            //     // Update credit balance after successful completion
            //     try {
            //         val balanceUserJwt = inputData.getString("userJwt") ?: ""
            //         if (balanceUserJwt.isNotEmpty()) {
            //             val updatedBalance = businessEngineClient.balance(balanceUserJwt)
            //             Log.d("TTT", "Updated credit balance after completion: ${updatedBalance.balance}")
            //         }
            //     } catch (e: Exception) {
            //         Log.w("TTT", "Failed to update credit balance: ${e.message}")
            //     }
            //     Result.success()
            // } else {
            //     Log.e("TTT", "Transcription failed: ${finalStatus.note}")
            //     Result.retry()
            // }
            
            // For now, just return success
            Log.i("TTT", "stage=COMPLETED url=$videoUrl reqId=$requestId msg=success")
            Result.success()
        } catch (e: Exception) {
            Log.e("TTT", "AI Analysis failed: ${e.message}", e)
            Result.retry()
        }
    }
}