package app.pluct.core.user

/**
 * Pluct-Core-User-01Display-01Formatter - Human-friendly device identity (single SSOT)
 * Scopes: Project, Core, User, DisplayFormatter
 */
object PluctCoreUser01Display01Formatter {

    fun friendlyDeviceLabel(rawUserId: String): String {
        val trimmed = rawUserId.trim()
        if (trimmed.isEmpty()) return "This phone"
        val suffix = trimmed.takeLast(4).takeIf { it.length == 4 && trimmed.length > 6 } ?: ""
        return if (suffix.isNotEmpty()) "This phone ···$suffix" else "This phone"
    }
}
