package app.pluct.premium

enum class PluctAdSlot { HISTORY_INLINE, TRANSCRIPT_BOTTOM, SETTINGS_PREMIUM_PREVIEW }
sealed class PluctAdResult { data class Ready(val title: String, val message: String) : PluctAdResult(); data object Suppressed : PluctAdResult() }

interface PluctAdProvider {
    suspend fun load(slot: PluctAdSlot, plan: String, serviceReady: Boolean, activeTranscription: Boolean): PluctAdResult
}

class PluctHouseAdProvider : PluctAdProvider {
    override suspend fun load(slot: PluctAdSlot, plan: String, serviceReady: Boolean, activeTranscription: Boolean): PluctAdResult {
        if (plan.equals("premium", true) || !serviceReady || activeTranscription) return PluctAdResult.Suppressed
        return PluctAdResult.Ready("Pluct Premium", "More uses, faster queue, no ads, and longer history.")
    }
}

object PluctPremiumPromptPolicy {
    private val milestones = setOf(7, 15, 30)

    fun shouldShow(
        successfulPlucts: Int,
        plan: String,
        serviceReady: Boolean,
        activeTranscription: Boolean,
        lastPromptAtMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (plan.equals("premium", true) || !serviceReady || activeTranscription) return false
        if (successfulPlucts in milestones) return nowMs - lastPromptAtMs >= 24 * 60 * 60 * 1000L
        return successfulPlucts > 30 && nowMs - lastPromptAtMs >= 30L * 24 * 60 * 60 * 1000L
    }
}
