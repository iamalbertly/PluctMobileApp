package app.pluct.ui.navigation

/**
 * Sealed class defining all navigation routes in Pluct.
 * 
 * Why sealed class: Provides type safety for navigation routes,
 * ensuring compile-time checking of valid destinations.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object Onboarding : Screen("onboarding")
    object Ingest : Screen("ingest?url={url}") {
        fun createRoute(url: String) = "ingest?url=${android.net.Uri.encode(url)}"
    }
}
