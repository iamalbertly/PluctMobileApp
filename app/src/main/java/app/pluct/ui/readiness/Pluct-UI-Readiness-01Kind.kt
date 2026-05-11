package app.pluct.ui.readiness

import android.content.Context
import app.pluct.core.network.PluctNetworkConnectivityChecker
import app.pluct.services.HealthStatus

/**
 * Pluct-UI-Readiness-01Kind - Readiness states for direct-to-value strip
 * Scopes: Project, UI, Readiness, Kind
 */
enum class PluctUIReadiness01Kind {
    CHECKING,
    VERIFY_FAILED,
    NO_NETWORK,
    NO_CREDITS,
    /** TTT unhealthy or Business Engine unreachable */
    SERVICE_DEGRADED,
    /** TTT degraded — still usable, expect delays */
    SERVICE_SLOW,
    READY
}

object PluctUIReadiness01Resolve {
    fun resolve(
        context: Context,
        balanceKnown: Boolean,
        balanceLoadFailed: Boolean,
        creditBalance: Int,
        freeUsesRemaining: Int,
        tttHealth: HealthStatus?,
        apiHealth: HealthStatus? = null
    ): PluctUIReadiness01Kind {
        if (!balanceKnown) return PluctUIReadiness01Kind.CHECKING
        if (balanceLoadFailed) return PluctUIReadiness01Kind.VERIFY_FAILED
        if (!PluctNetworkConnectivityChecker.isNetworkAvailable(context)) {
            return PluctUIReadiness01Kind.NO_NETWORK
        }
        if (apiHealth == HealthStatus.UNHEALTHY) {
            return PluctUIReadiness01Kind.SERVICE_DEGRADED
        }
        if (tttHealth == HealthStatus.UNHEALTHY) {
            return PluctUIReadiness01Kind.SERVICE_DEGRADED
        }
        if (tttHealth == HealthStatus.DEGRADED) {
            return PluctUIReadiness01Kind.SERVICE_SLOW
        }
        if (creditBalance < 1 && freeUsesRemaining < 1) {
            return PluctUIReadiness01Kind.NO_CREDITS
        }
        return PluctUIReadiness01Kind.READY
    }
}
