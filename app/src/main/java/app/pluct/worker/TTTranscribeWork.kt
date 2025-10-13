package app.pluct.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.pluct.api.EngineApiProvider
import app.pluct.config.AppConfig
import kotlinx.coroutines.delay

class TTTranscribeWork(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    private val api = EngineApiProvider.instance
    private val userId = AppConfig.userId

    private suspend fun stage(s: String, url: String, reqId: String? = null, msg: String? = null, pct: Int? = null) {
        setProgress(workDataOf("stage" to s, "percent" to (pct ?: -1)))
        Log.i("TTT", "stage=$s url=$url reqId=${reqId ?: "-"} msg=${msg ?: ""}")
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()
        
        try {
            stage("VENDING_TOKEN", url, msg = "requesting")
            val tok = api.vendToken(mapOf("userId" to userId)).body()?.get("token") as? String
                ?: return Result.retry()

            stage("REQUEST_SUBMITTED", url, msg = "proxy")
            val tr = api.transcribe("Bearer $tok", mapOf("url" to url)).body()
            val reqId = (tr?.get("request_id") as? String) ?: return Result.retry()

            stage("REMOTE_ACK", url, reqId, "accepted", 25)

            var done = false
            var lastPct = 30
            while (!done) {
                delay(1000)
                val st = api.status("Bearer $tok", reqId).body() ?: continue
                val phase = st["phase"]?.toString() ?: ""
                val pct = (st["percent"] as? Number)?.toInt() ?: (++lastPct).coerceAtMost(95)
                when (phase) {
                    "TRANSCRIBING" -> stage("TRANSCRIBING", url, reqId, pct = pct)
                    "SUMMARIZING" -> stage("SUMMARIZING", url, reqId, pct = pct)
                    "COMPLETED" -> {
                        val text = st["text"]?.toString() ?: ""
                        persistResult(url, reqId, text)
                        stage("COMPLETED", url, reqId, pct = 100)
                        done = true
                    }
                    "FAILED" -> {
                        stage("FAILED", url, reqId, st["note"]?.toString())
                        return Result.retry()
                    }
                }
            }
            return Result.success()
        } catch (t: Throwable) {
            stage("FAILED", url, msg = t.message)
            return Result.retry()
        }
    }

    private fun persistResult(url: String, reqId: String, text: String) {
        // TODO: write to Room DB (Transcripts table)
        Log.i("TTT", "Persisting result for url=$url reqId=$reqId textLength=${text.length}")
    }

    companion object {
        fun input(url: String) = workDataOf("url" to url)
    }
}