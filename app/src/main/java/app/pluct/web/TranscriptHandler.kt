package app.pluct.web

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import app.pluct.data.repository.PluctRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles transcript processing and saving
 */
class TranscriptHandler @Inject constructor(
    private val repository: PluctRepository,
    @ApplicationContext private val appContext: Context
) {
    companion object {
        private const val TAG = "PluctWeb"
    }
    
    fun handleTranscriptReady(videoId: String, transcript: String, language: String?, startTime: Long, providerUsed: String? = null) {
        Log.d(TAG, "WV:A:transcript_received_handler len=${transcript.length} vid=" + videoId + " provider=" + (providerUsed ?: "unknown"))
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.saveTranscript(videoId, transcript, language)
                repository.markUrlAsValid(videoId)
                Log.i(TAG, "WV:A:transcript_saved videoId=" + videoId + " provider=" + (providerUsed ?: "unknown"))
            } catch (e: Exception) {
                Log.e(TAG, "WV:A:transcript_save_error ${e.message}", e)
            }
        }
    }
    
    fun saveToClipboard(label: String, text: String) {
        try {
            val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            cm.setPrimaryClip(clip)
            Log.i(TAG, "WV:A:clipboard_saved label=$label len=" + text.length)
        } catch (e: Exception) {
            Log.e(TAG, "WV:A:clipboard_error ${e.message}", e)
        }
    }
}