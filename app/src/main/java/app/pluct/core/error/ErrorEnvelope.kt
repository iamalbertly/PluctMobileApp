package app.pluct.core.error

/**
 * Error envelope for structured error handling
 */
data class ErrorEnvelope(
    val code: String,
    val message: String,
    val details: Map<String, Any> = emptyMap()
)
