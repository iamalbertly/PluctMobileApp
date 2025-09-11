package app.pluct.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    @Volatile private var initialized = false
    private lateinit var logFile: File
    private val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        if (initialized) return
        logFile = File(context.filesDir, "pluct_debug.log")
        if (!logFile.exists()) logFile.createNewFile()
        initialized = true
    }

    fun clear() {
        if (!initialized) return
        logFile.writeText("")
    }

    fun log(message: String) {
        if (!initialized) return
        val ts = sdf.format(Date())
        logFile.appendText("[$ts] $message\n")
        // Optional: truncate if too large (~256KB)
        if (logFile.length() > 262144) {
            val content = logFile.readText()
            val keep = content.takeLast(200_000)
            logFile.writeText(keep)
        }
    }
}
