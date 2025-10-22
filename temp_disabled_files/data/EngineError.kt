package app.pluct.data

/**
 * EngineError - Business Engine API error types
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
sealed class EngineError(message: String) : Exception(message) {
    class InvalidUrl(message: String = "Invalid URL provided") : EngineError(message)
    class Auth(message: String = "Authentication failed") : EngineError(message)
    class InsufficientCredits(message: String = "Insufficient credits") : EngineError(message)
    class RateLimited(message: String = "Rate limit exceeded") : EngineError(message)
    class ServiceUnavailable(message: String = "Service unavailable") : EngineError(message)
    class NetworkError(message: String = "Network error") : EngineError(message)
    class Timeout(message: String = "Request timeout") : EngineError(message)
    class Unknown(message: String = "Unknown error") : EngineError(message)
}
