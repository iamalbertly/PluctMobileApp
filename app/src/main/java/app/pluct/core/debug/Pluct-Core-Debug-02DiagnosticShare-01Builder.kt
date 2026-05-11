package app.pluct.core.debug

import app.pluct.BuildConfig
import app.pluct.data.entity.DebugLogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pluct-Core-Debug-02DiagnosticShare-01Builder - One bundle for support (SSOT text builder)
 * Scopes: Project, Core, Debug, DiagnosticShareBuilder
 */
object PluctCoreDebug02DiagnosticShare01Builder {

    fun buildText(
        logs: List<DebugLogEntry>,
        categoryBreakdown: String = "",
        maxEntries: Int = 40
    ): String = buildString {
        appendLine("=== Pluct diagnostic ===")
        appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'", Locale.US).format(Date())}")
        if (categoryBreakdown.isNotBlank()) {
            appendLine()
            appendLine(categoryBreakdown.trim())
        }
        appendLine("--- Recent logs (newest last) ---")
        logs.takeLast(maxEntries).forEach { e ->
            appendLine("[${e.level}] ${e.timestamp} ${e.category}/${e.operation}")
            appendLine(e.message)
            if (e.requestUrl.isNotBlank()) appendLine("  url: ${e.requestUrl}")
            appendLine()
        }
        appendLine("=== End ===")
    }
}
