package app.pluct.services

/**
 * SSOT for in-app + worker progress heartbeat copy and pacing (Customer: same rhythm in shade and UI; Trust: no divergent progress math).
 */
object PluctCoreTranscription02ProgressHeartbeat01Policy {
    const val TICK_MS = 4000L

    val LABELS = listOf(
        "Video -> Text",
        "Link -> Ready",
        "Video -> Audio",
        "Audio -> Text",
        "Text -> Soon",
        "Almost done"
    )

    fun nextProgress(current: Int): Int =
        (current + if (current < 72) 10 else 3).coerceAtMost(91)
}
