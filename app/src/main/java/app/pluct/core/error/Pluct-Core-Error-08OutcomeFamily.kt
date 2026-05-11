package app.pluct.core.error

import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem

/**
 * Pluct-Core-Error-08OutcomeFamily - Single taxonomy for user-visible failure outcomes
 * Scopes: Project, Core, Error, OutcomeFamily
 */
enum class PluctCoreError08OutcomeFamily {
    CREDITS,
    LINK,
    WAIT,
    SERVER_NET,
    UNKNOWN;

    companion object {
        fun fromVideoItem(video: VideoItem): PluctCoreError08OutcomeFamily {
            if (video.status != ProcessingStatus.FAILED) return UNKNOWN
            val r = video.failureReason.orEmpty() + " " + video.errorDetails.orEmpty()
            return when {
                r.contains("402", ignoreCase = true) ||
                    r.contains("insufficient", ignoreCase = true) ||
                    r.contains("credit", ignoreCase = true) -> CREDITS
                r.contains("401", ignoreCase = true) ||
                    r.contains("unauthorized", ignoreCase = true) ||
                    r.contains("jwt", ignoreCase = true) ||
                    r.contains("session", ignoreCase = true) -> WAIT
                r.contains("invalid", ignoreCase = true) ||
                    r.contains("400", ignoreCase = true) ||
                    r.contains("private", ignoreCase = true) ||
                    r.contains("not found", ignoreCase = true) -> LINK
                r.contains("network", ignoreCase = true) ||
                    r.contains("timeout", ignoreCase = true) ||
                    r.contains("connection", ignoreCase = true) ||
                    r.contains("500", ignoreCase = true) ||
                    r.contains("503", ignoreCase = true) ||
                    r.contains("502", ignoreCase = true) -> SERVER_NET
                else -> UNKNOWN
            }
        }

        fun shortLabel(family: PluctCoreError08OutcomeFamily): String = when (family) {
            CREDITS -> "Credits"
            LINK -> "Link"
            WAIT -> "Wait"
            SERVER_NET -> "Network"
            UNKNOWN -> "Issue"
        }
    }
}
