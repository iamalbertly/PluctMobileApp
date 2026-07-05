package app.pluct.shared

enum class PluctApiErrorCategory {
    VALIDATION,
    AUTHENTICATION,
    RATE_LIMIT,
    NETWORK,
    SERVER,
    SERVICE_COOLDOWN,
    UNKNOWN
}

data class PluctApiErrorMapping(
    val category: PluctApiErrorCategory,
    val userMessage: String,
    val retryable: Boolean
)

object PluctApiErrorMapper {
    fun map(statusCode: Int?, message: String? = null): PluctApiErrorMapping {
        val normalized = message.orEmpty().lowercase()
        return when {
            normalized.contains("service_cooldown") ->
                PluctApiErrorMapping(PluctApiErrorCategory.SERVICE_COOLDOWN, "Service is cooling down. Try again shortly.", true)
            statusCode == 401 || statusCode == 403 || normalized.contains("auth") || normalized.contains("token") ->
                PluctApiErrorMapping(PluctApiErrorCategory.AUTHENTICATION, "Session expired. Try again in a moment.", true)
            statusCode == 408 || normalized.contains("timeout") || normalized.contains("network") || normalized.contains("connection") ->
                PluctApiErrorMapping(PluctApiErrorCategory.NETWORK, "Network issue. Check your connection and retry.", true)
            statusCode == 429 || normalized.contains("rate limit") ->
                PluctApiErrorMapping(PluctApiErrorCategory.RATE_LIMIT, "Too many requests. Try again shortly.", true)
            statusCode == 426 || normalized.contains("app_update_required") || normalized.contains("update pluct") ->
                PluctApiErrorMapping(PluctApiErrorCategory.VALIDATION, "Update Pluct to continue.", false)
            statusCode in 400..499 ->
                PluctApiErrorMapping(PluctApiErrorCategory.VALIDATION, "This request cannot be processed as sent.", false)
            statusCode != null && statusCode >= 500 ->
                PluctApiErrorMapping(PluctApiErrorCategory.SERVER, "Pluct service is temporarily unavailable.", true)
            else ->
                PluctApiErrorMapping(PluctApiErrorCategory.UNKNOWN, message ?: "Something went wrong.", false)
        }
    }
}
