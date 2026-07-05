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
        maxEntries: Int = 40,
        includeSensitiveContent: Boolean = false
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
            appendLine(if (includeSensitiveContent) e.message else redact(e.message))
            if (includeSensitiveContent && e.requestUrl.isNotBlank()) appendLine("  url: ${e.requestUrl}")
            appendLine()
        }
        appendLine("=== End ===")
    }

    private fun redact(value: String): String = value
        .replace(Regex("https?://\\S+", RegexOption.IGNORE_CASE), "<url-redacted>")
        .replace(Regex("(?i)(bearer\\s+|token[=: ]+|secret[=: ]+|api[_-]?key[=: ]+)\\S+"), "$1<redacted>")
}
