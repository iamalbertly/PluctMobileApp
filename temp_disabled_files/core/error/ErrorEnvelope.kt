package app.pluct.core.error

data class ErrorEnvelope(
    val code: String,              // e.g. AUTH_401, CREDITS_402, SCOPE_403, RATE_429, SERVER_500, NET_TIMEOUT, NET_IO
    val message: String,           // human readable
    val details: Map<String, Any?> = emptyMap(),
    val requestId: String? = null, // if available from server
    val source: String = "client", // client|engine
    val timestamp: Long = System.currentTimeMillis(),
    val severity: ErrorSeverity = ErrorSeverity.ERROR,
    val context: String? = null    // additional context about where the error occurred
)

enum class ErrorSeverity {
    INFO, WARNING, ERROR, CRITICAL
}
