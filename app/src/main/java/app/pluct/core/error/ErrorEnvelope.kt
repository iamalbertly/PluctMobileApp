package app.pluct.core.error

/**
 * Pluct-Core-Error-ErrorEnvelope - Simple error envelope for basic error handling
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
data class ErrorEnvelope(
    val type: ErrorType,
    val title: String,
    val message: String,
    val technicalDetails: String? = null,
    val userAction: String? = null,
    val error: Throwable? = null,
    val recoveryAttempted: Boolean = false
)

enum class ErrorType {
    NETWORK,
    AUTHENTICATION,
    VALIDATION,
    API,
    UNKNOWN
}

enum class ErrorSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
