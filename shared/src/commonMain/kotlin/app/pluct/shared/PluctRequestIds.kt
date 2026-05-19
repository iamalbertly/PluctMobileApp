package app.pluct.shared

import kotlin.random.Random

object PluctRequestIds {
    fun generate(prefix: String = "req"): String {
        val safePrefix = prefix.ifBlank { "req" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return "${safePrefix}_${randomHex(24)}"
    }

    fun generateCreditRequestId(): String = generate("credit_req")

    private fun randomHex(length: Int): String {
        val chars = "0123456789abcdef"
        return buildString(length) {
            repeat(length) {
                append(chars[Random.nextInt(chars.length)])
            }
        }
    }
}
