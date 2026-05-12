package app.pluct.notification

/**
 * Single place for short notification copy (Customer: consistent tone; no duplicate when strings).
 */
object PluctNotification02Copy01Formatter {
    fun progressTitle(progress: Int): String = "$progress% -> Text"

    fun progressText(message: String): String {
        val lower = message.lowercase()
        return when {
            "network" in lower || "connection" in lower || "reconnect" in lower -> "Wi-Fi -> Resume"
            "server" in lower || "busy" in lower -> "Wait -> Retry"
            "download" in lower -> "Video -> Audio"
            "extract" in lower || "audio" in lower -> "Audio -> Text"
            "transcrib" in lower -> "Audio -> Text"
            "final" in lower || "complete" in lower -> "Almost done"
            "start" in lower || "prepar" in lower -> "Video -> Text"
            else -> message.take(48)
        }
    }

    fun errorText(error: String): String {
        val lower = error.lowercase()
        return when {
            "credit" in lower || "insufficient" in lower -> "Add credits -> Continue"
            "network" in lower || "connection" in lower || "resolve host" in lower -> "No internet -> Saved"
            "tttranscribe" in lower || "service" in lower || "server" in lower || "500" in lower ->
                "Try again soon. Video saved."
            "timeout" in lower || "still working" in lower -> "Still working. Check soon."
            else -> error.take(72)
        }
    }
}
