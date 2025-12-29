package app.pluct.core.debug

import android.content.Context

/**
 * Pluct-Core-Debug-02ADBDetection - ADB connection detection utility
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Detects if ADB is connected to prevent false positive timeout errors
 */
object PluctCoreADBDetection {
    /**
     * Check if ADB is connected via TCP/IP
     * Returns true if ADB TCP port is set and non-zero
     */
    fun isAdbConnected(context: Context): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("getprop service.adb.tcp.port")
            val output = process.inputStream.bufferedReader().readText().trim()
            output.isNotEmpty() && output != "0" && output.toIntOrNull() != null
        } catch (e: Exception) {
            // If we can't check, assume not connected to avoid false positives
            false
        }
    }
}

