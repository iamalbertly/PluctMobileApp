package app.pluct.services

import android.util.Log
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Pluct-Core-API-00ServerTimeSync - Tracks server time offset from Date header
 * Helps mitigate device clock skew for JWT issuance.
 */
object PluctCoreAPIServerTimeSync {

    private const val TAG = "PluctServerTimeSync"
    private const val LOG_SKEW_THRESHOLD_SECONDS = 30L
    private const val MAX_REASONABLE_SKEW_SECONDS = 6 * 60 * 60L
    private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }

    @Volatile
    private var offsetSeconds: Long? = null

    fun updateFromHttpDate(dateHeader: String?) {
        if (dateHeader.isNullOrBlank()) return
        val serverEpochSeconds = parseDateHeader(dateHeader) ?: return
        val localEpochSeconds = System.currentTimeMillis() / 1000
        val skewSeconds = serverEpochSeconds - localEpochSeconds

        if (kotlin.math.abs(skewSeconds) > MAX_REASONABLE_SKEW_SECONDS) {
            Log.w(TAG, "Ignoring unreasonable server time skew: ${skewSeconds}s")
            return
        }

        offsetSeconds = skewSeconds

        if (kotlin.math.abs(skewSeconds) >= LOG_SKEW_THRESHOLD_SECONDS) {
            Log.w(TAG, "Detected device clock skew: ${skewSeconds}s (server ahead if positive)")
        } else {
            Log.d(TAG, "Server time sync updated (skew ${skewSeconds}s)")
        }
    }

    fun nowEpochSeconds(): Long {
        val now = System.currentTimeMillis() / 1000
        return now + (offsetSeconds ?: 0)
    }

    private fun parseDateHeader(dateHeader: String): Long? {
        return try {
            val parsed = dateFormat.parse(dateHeader) ?: return null
            parsed.time / 1000
        } catch (e: ParseException) {
            Log.w(TAG, "Failed to parse Date header: ${e.message}")
            null
        }
    }
}
