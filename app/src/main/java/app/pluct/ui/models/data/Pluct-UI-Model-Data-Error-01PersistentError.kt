package app.pluct.ui.models.data

/**
 * Pluct-UI-Model-Data-Error-01PersistentError
 * Data class for persistent error state that survives component lifecycle
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[SubScope]-[Responsibility]
 */
data class PersistentError(
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val url: String? = null,
    val error: Throwable? = null,
    val errorCode: String? = null,
    val httpStatus: Int? = null,
    val category: String? = null
)






