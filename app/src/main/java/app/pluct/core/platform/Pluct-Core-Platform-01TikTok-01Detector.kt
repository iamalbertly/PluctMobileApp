package app.pluct.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Pluct-Core-Platform-01TikTok-01Detector
 * Single source of truth for TikTok detection and launching.
 * Follows naming: [Project]-[Core]-[Platform]-[TikTok]-[Sequence][Detector]
 * 6 scope layers: Project, Core, Platform, TikTok, Sequence, Detector
 *
 * CONSOLIDATES duplicate TikTok logic from:
 * - Pluct-UI-Component-07Onboarding-01Tutorial-01Flow.kt (3 occurrences)
 */
object PluctCorePlatform01TikTok01Detector {
    private const val TAG = "TikTokDetector"

    // All known TikTok package names
    private val TIKTOK_PACKAGES = listOf(
        "com.zhiliaoapp.musically",  // Standard TikTok (most common)
        "com.ss.android.ugc.aweme"   // Alternative/regional package
    )

    /**
     * Check if TikTok is installed on the device.
     * Uses getLaunchIntentForPackage for reliable detection.
     */
    fun isInstalled(context: Context): Boolean {
        return TIKTOK_PACKAGES.any { packageName ->
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                val installed = launchIntent != null
                if (installed) {
                    Log.d(TAG, "TikTok detected: $packageName")
                }
                installed
            } catch (e: Exception) {
                Log.d(TAG, "TikTok check failed for $packageName: ${e.message}")
                false
            }
        }
    }

    /**
     * Launch TikTok app or open Play Store if not installed.
     * Returns true if successfully launched TikTok, false if opened Play Store.
     */
    fun launchOrInstall(context: Context): Boolean {
        // Try to launch installed TikTok
        for (packageName in TIKTOK_PACKAGES) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    Log.d(TAG, "Launching TikTok: $packageName")
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch TikTok $packageName: ${e.message}")
            }
        }

        // TikTok not installed - open Play Store
        Log.d(TAG, "TikTok not found, opening Play Store")
        openPlayStore(context)
        return false
    }

    /**
     * Open Play Store to TikTok listing
     */
    private fun openPlayStore(context: Context) {
        try {
            val playStoreIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=${TIKTOK_PACKAGES[0]}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playStoreIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Play Store failed, trying web: ${e.message}")
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${TIKTOK_PACKAGES[0]}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Web Play Store also failed: ${e2.message}")
            }
        }
    }

    /**
     * Get the primary TikTok package name (for share intents)
     */
    fun getPrimaryPackage(): String = TIKTOK_PACKAGES[0]
}
