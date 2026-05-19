package app.pluct.shared

data class PluctApiError(
    val statusCode: Int? = null,
    val message: String = "",
    val exceptionType: String = "",
    val retryableHint: Boolean = false
)

object PluctRetryPolicy {
    fun isRetryable(error: PluctApiError?): Boolean {
        if (error == null) return false
        val message = error.message.lowercase()
        if (isMessageRetryable(message) || isExceptionTypeRetryable(error.exceptionType)) return true
        val status = error.statusCode
        if (status == 408 || status == 429 || (status != null && status >= 500)) return true
        if (status in 400..499) return false
        return error.retryableHint
    }

    fun shouldCountForCircuitBreaker(error: PluctApiError?): Boolean {
        val status = error?.statusCode
        if (status in 400..499 && status != 408 && status != 429) return false
        return isRetryable(error)
    }

    fun calculateRetryDelayMs(attemptNumber: Int, baseDelayMs: Long = 1000L, maxDelayMs: Long = 30000L): Long {
        val normalizedAttempt = attemptNumber.coerceAtLeast(1).coerceAtMost(30)
        val exponentialDelay = baseDelayMs * (1L shl (normalizedAttempt - 1))
        return exponentialDelay.coerceAtMost(maxDelayMs)
    }

    private fun isMessageRetryable(message: String): Boolean {
        return message.contains("timeout") ||
            message.contains("timed out") ||
            message.contains("network") ||
            message.contains("connection") ||
            message.contains("unavailable") ||
            message.contains("temporarily") ||
            message.contains("reset by peer") ||
            message.contains("no route to host") ||
            message.contains("host unreachable")
    }

    private fun isExceptionTypeRetryable(exceptionType: String): Boolean {
        val retryableTypes = listOf(
            "SocketTimeoutException",
            "ConnectException",
            "UnknownHostException",
            "SocketException",
            "EOFException"
        )
        return retryableTypes.any { exceptionType.contains(it) }
    }
}
